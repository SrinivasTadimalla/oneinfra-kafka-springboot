package com.srikar.kafka.service;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import com.srikar.kafka.config.KafkaAdminConfig;
import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.dto.KafkaClusterHealthDto;
import com.srikar.kafka.dto.KafkaClusterMetaDto;
import com.srikar.kafka.dto.KafkaClusterOverviewDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaClusterSnapshotService {

    private final KafkaClusterRepository clusterRepo;
    private final KafkaBootstrapResolver bootstrapResolver;   // Redis-first
    private final KafkaAdminHealthService healthService;

    // ✅ AdminClient factory (DB bootstrap + YAML SSL)
    private final KafkaAdminConfig kafkaAdminConfig;

    public List<KafkaClusterOverviewDto> listClustersWithHealth() {

        return clusterRepo.findAllByOrderByNameAsc().stream()
                .map(cluster -> {
                    KafkaClusterMetaDto meta = KafkaClusterMetaDto.builder()
                            .name(cluster.getName())
                            .environment(cluster.getEnvironment())
                            .bootstrapServers(cluster.getBootstrapServers()) // DB value for UI display
                            .enabled(cluster.isEnabled())
                            .updatedAt(cluster.getUpdatedAt())
                            .build();

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
                        health = healthService.probe(bootstrap);
                    }

                    return KafkaClusterOverviewDto.builder()
                            .meta(meta)
                            .health(health)
                            .build();
                })
                .toList();
    }

    /**
     * Snapshot is a richer payload than "health":
     * - controller + nodes
     * - topics list + count
     */
    public Map<String, Object> snapshot(String clusterName) {

        // ✅ Redis-first (your design)
        String bootstrap = bootstrapResolver.resolve(clusterName);

        try (AdminClient admin = kafkaAdminConfig.createAdminClient(bootstrap)) {

            DescribeClusterResult dcr = admin.describeCluster();

            Node controller = dcr.controller().get();
            List<Node> nodes = dcr.nodes().get().stream()
                    .sorted(Comparator.comparingInt(Node::id))
                    .toList();

            var topicsResult = admin.listTopics();
            List<String> topics = topicsResult.names().get().stream()
                    .sorted()
                    .toList();

            return Map.of(
                    "cluster", clusterName,
                    "bootstrap", bootstrap,
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
                    "error", e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }
    }
}
