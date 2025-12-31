package com.srikar.kafka.service;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import com.srikar.kafka.config.KafkaAdminClientFactory;
import com.srikar.kafka.config.KafkaAdminProperties;
import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.dto.cluster.KafkaClusterHealthDto;
import com.srikar.kafka.dto.cluster.KafkaClusterMetaDto;
import com.srikar.kafka.dto.cluster.KafkaClusterOverviewDto;
import com.srikar.kafka.dto.cluster.KafkaKraftControlPlaneDto;
import com.srikar.kafka.entity.KafkaClusterEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaClusterSnapshotService {

    private final KafkaClusterRepository clusterRepo;
    private final KafkaBootstrapResolver bootstrapResolver;   // Redis-first
    private final KafkaAdminHealthService healthService;

    // ✅ Centralized AdminClient factory + YAML properties
    private final KafkaAdminClientFactory adminFactory;
    private final KafkaAdminProperties props;

    // ✅ NEW: KRaft controller health via JMX
    private final KafkaKraftJmxHealthService kraftJmxHealthService;

    /**
     * ✅ Overview list:
     * - returns DB meta + live health
     * - caches Kafka internal clusterId (describeCluster().clusterId()) into DB column kafka_cluster_id
     *   so UI can display it even if Kafka is down later.
     * - ✅ Adds control-plane (KRaft) info from JMX (bare metal controller)
     */
    @Transactional
    public List<KafkaClusterOverviewDto> listClustersWithHealth() {

        return clusterRepo.findAllByOrderByNameAsc().stream()
                .map(cluster -> {

                    // -------------------------------------------------------
                    // Build meta (DB)
                    // -------------------------------------------------------
                    KafkaClusterMetaDto meta = KafkaClusterMetaDto.builder()
                            .id(cluster.getId())
                            .name(cluster.getName())
                            .environment(cluster.getEnvironment())
                            .bootstrapServers(cluster.getBootstrapServers()) // DB value for UI display
                            .enabled(cluster.isEnabled())
                            .kafkaClusterId(cluster.getKafkaClusterId())     // DB cached value
                            .updatedAt(cluster.getUpdatedAt())
                            .build();

                    // -------------------------------------------------------
                    // Health (live) - broker/data plane
                    // -------------------------------------------------------
                    KafkaClusterHealthDto health;
                    if (!cluster.isEnabled()) {
                        health = KafkaClusterHealthDto.builder()
                                .status("UNKNOWN")
                                .observedAt(Instant.now())
                                .brokerCount(0)
                                .brokers(List.of())
                                .error("Cluster disabled in DB")
                                .build();
                    } else {
                        String bootstrap = bootstrapResolver.resolve(cluster.getName()); // Redis-first

                        // ✅ Cache Kafka internal clusterId into DB (best-effort)
                        cacheKafkaClusterIdIfNeeded(cluster, bootstrap);

                        // Existing live probe (uses adminFactory + props now)
                        health = healthService.probe(bootstrap);
                    }

                    // -------------------------------------------------------
                    // ✅ NEW: Control Plane (KRaft controller) - from JMX
                    // -------------------------------------------------------
                    KafkaKraftControlPlaneDto controlPlane = buildControlPlane(cluster);

                    // Keep meta.kafkaClusterId in sync for this response
                    KafkaClusterMetaDto metaFinal = meta.toBuilder()
                            .kafkaClusterId(cluster.getKafkaClusterId())
                            .build();

                    return KafkaClusterOverviewDto.builder()
                            .meta(metaFinal)
                            .health(health)
                            .controlPlane(controlPlane) // ✅ NEW
                            .build();
                })
                .toList();
    }

    /**
     * ✅ Snapshot is a richer payload than "health":
     * - controller + nodes
     * - topics list + count
     * - caches kafka_cluster_id into DB as well (best-effort)
     */
    @Transactional
    public Map<String, Object> snapshot(String clusterName) {

        String bootstrap = bootstrapResolver.resolve(clusterName);
        KafkaClusterEntity cluster = clusterRepo.findByNameIgnoreCase(clusterName).orElse(null);

        long timeoutMs = safeTimeoutMs();

        try (AdminClient admin = adminFactory.create(bootstrap)) {

            DescribeClusterResult dcr = admin.describeCluster();

            // ✅ Cache Kafka internal clusterId into DB (best-effort)
            if (cluster != null) {
                cacheKafkaClusterIdIfNeeded(cluster, admin, timeoutMs);
            }

            String kafkaClusterId = safeGetClusterId(dcr, timeoutMs);

            Node controller = dcr.controller().get(timeoutMs, TimeUnit.MILLISECONDS);

            List<Node> nodes = dcr.nodes().get(timeoutMs, TimeUnit.MILLISECONDS).stream()
                    .sorted(Comparator.comparingInt(Node::id))
                    .toList();

            var topicsResult = admin.listTopics();
            List<String> topics = topicsResult.names().get(timeoutMs, TimeUnit.MILLISECONDS).stream()
                    .sorted()
                    .toList();

            return Map.of(
                    "cluster", clusterName,
                    "bootstrap", bootstrap,
                    "kafkaClusterId", kafkaClusterId,
                    "observedAt", Instant.now().toString(),
                    "controller", controller == null ? null : Map.of(
                            "id", controller.id(),
                            "host", controller.host(),
                            "port", controller.port(),
                            "rack", controller.rack()
                    ),
                    "nodes", nodes.stream().map(n -> Map.of(
                            "id", n.id(),
                            "host", n.host(),
                            "port", n.port(),
                            "rack", n.rack()
                    )).toList(),
                    "topicCount", topics.size(),
                    "topics", topics
            );

        } catch (Exception e) {
            log.error("Snapshot failed for cluster={} bootstrap={}", clusterName, bootstrap, e);

            return Map.of(
                    "cluster", clusterName,
                    "bootstrap", bootstrap,
                    "observedAt", Instant.now().toString(),
                    "error", e.getClass().getSimpleName() + ": " + safeMsg(e)
            );
        }
    }

    // -------------------------------------------------------
    // ✅ NEW: Control plane builder
    // -------------------------------------------------------

    private KafkaKraftControlPlaneDto buildControlPlane(KafkaClusterEntity cluster) {

        // ✅ For now, your KRaft controller is bare metal and shared across clusters in LAB.
        // Later we can move these into DB/config per cluster.
        final String nodeLabel = "oneinfra-host";
        final int quorumNodeId = 0;

        final String controllerListener = "192.168.66.1:9093"; // your controller listener
        final String jmxHost = "192.168.66.1";
        final int jmxPort = 9999;

        // If cluster disabled, keep it UNKNOWN
        if (!cluster.isEnabled()) {
            return KafkaKraftControlPlaneDto.builder()
                    .node(nodeLabel)
                    .role("CONTROLLER")
                    .listener(controllerListener)
                    .quorumNodeId(quorumNodeId)
                    .mode("SINGLE_NODE_QUORUM")
                    .status("UNKNOWN")
                    .observedAt(Instant.now().toString())
                    .source("JMX")
                    .error("Cluster disabled in DB")
                    .build();
        }

        KafkaKraftJmxHealthService.Result r =
                kraftJmxHealthService.probe(jmxHost, jmxPort, (int) safeTimeoutMs());

        return KafkaKraftControlPlaneDto.builder()
                .node(nodeLabel)
                .role("CONTROLLER")
                .listener(controllerListener)
                .quorumNodeId(quorumNodeId)
                .mode("SINGLE_NODE_QUORUM")
                .status(r.getStatus())
                .observedAt(r.getObservedAt())
                .source("JMX")
                .error(r.getError())
                .build();
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private long safeTimeoutMs() {
        Integer ms = props.getDefaultApiTimeoutMs();
        return (ms == null || ms < 1000) ? 15000L : ms.longValue();
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) return "";
        return m.length() > 500 ? m.substring(0, 500) : m;
    }

    /**
     * Best-effort: opens an AdminClient and caches kafka_cluster_id into DB if missing or changed.
     * Does NOT fail the main flow if Kafka is down.
     */
    private void cacheKafkaClusterIdIfNeeded(KafkaClusterEntity cluster, String bootstrap) {
        long timeoutMs = safeTimeoutMs();
        try (AdminClient admin = adminFactory.create(bootstrap)) {
            cacheKafkaClusterIdIfNeeded(cluster, admin, timeoutMs);
        } catch (Exception e) {
            log.debug("Unable to fetch/cache Kafka clusterId for cluster={} bootstrap={}",
                    cluster.getName(), bootstrap, e);
        }
    }

    /**
     * Best-effort: caches kafka_cluster_id using an existing AdminClient.
     */
    private void cacheKafkaClusterIdIfNeeded(KafkaClusterEntity cluster, AdminClient admin, long timeoutMs) {
        try {
            String kafkaClusterId = admin.describeCluster()
                    .clusterId()
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            if (kafkaClusterId == null || kafkaClusterId.isBlank()) return;

            if (cluster.getKafkaClusterId() == null || !cluster.getKafkaClusterId().equals(kafkaClusterId)) {
                cluster.setKafkaClusterId(kafkaClusterId);
                clusterRepo.save(cluster);
                log.info("Cached kafka_cluster_id={} for cluster name={}", kafkaClusterId, cluster.getName());
            }
        } catch (Exception e) {
            log.debug("Unable to fetch/cache Kafka clusterId for cluster={}", cluster.getName(), e);
        }
    }

    /**
     * Safe helper for embedding into snapshot response (doesn't throw).
     */
    private String safeGetClusterId(DescribeClusterResult dcr, long timeoutMs) {
        try {
            return dcr.clusterId().get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return null;
        }
    }
}
