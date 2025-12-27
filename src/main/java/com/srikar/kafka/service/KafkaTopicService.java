package com.srikar.kafka.service;

import com.srikar.kafka.config.KafkaAdminClientFactory;
import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.db.KafkaTopicRepository;
import com.srikar.kafka.dto.topic.TopicCreateRequest;
import com.srikar.kafka.dto.topic.TopicDetail;
import com.srikar.kafka.dto.topic.TopicSummary;
import com.srikar.kafka.dto.topic.TopicUpdateRequest;
import com.srikar.kafka.entity.KafkaClusterEntity;
import com.srikar.kafka.entity.KafkaTopicEntity;
import com.srikar.kafka.enums.KafkaTopicStatus;
import com.srikar.kafka.exception.DomainValidationException;
import com.srikar.kafka.exception.DuplicateTopicException;
import com.srikar.kafka.exception.KafkaOperationException;
import com.srikar.kafka.exception.ResourceNotFoundException;
import com.srikar.kafka.exception.TopicNotFoundException;
import com.srikar.kafka.utilities.TopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaTopicService {

    private final KafkaTopicRepository topicRepo;
    private final KafkaClusterRepository clusterRepo;
    private final KafkaAdminClientFactory adminFactory;

    // -------------------------------------------------------
    // CREATE Topic (Kafka + DB)
    // -------------------------------------------------------
    @PreAuthorize("hasRole('KAFKA_ADMIN')")
    @Transactional
    public TopicDetail createTopic(TopicCreateRequest req) {

        UUID clusterId = req.getClusterId();
        String topicName = req.getTopicName();

        KafkaClusterEntity cluster = clusterRepo.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Kafka cluster not found: " + clusterId));

        if (!cluster.isEnabled()) {
            throw new DomainValidationException("Kafka cluster is disabled: " + cluster.getName());
        }

        if (topicRepo.existsByCluster_IdAndTopicName(clusterId, topicName)) {
            throw new DuplicateTopicException(clusterId, topicName);
        }

        try (AdminClient admin = adminFactory.create(cluster.getBootstrapServers())) {

            // ✅ NEW: cache Kafka internal clusterId into DB (safe + non-blocking)
            cacheKafkaClusterIdIfNeeded(admin, cluster);

            NewTopic newTopic = new NewTopic(
                    topicName,
                    req.getPartitions(),
                    req.getReplicationFactor()
            );

            admin.createTopics(List.of(newTopic))
                    .all()
                    .get(8, TimeUnit.SECONDS);

            if (req.getConfigs() != null && !req.getConfigs().isEmpty()) {
                applyTopicConfigs(admin, topicName, req.getConfigs());
            }

        } catch (Exception e) {
            throw new KafkaOperationException(
                    "Kafka topic creation failed: " + topicName + " (cluster=" + cluster.getName() + ")",
                    e
            );
        }

        KafkaTopicEntity saved = topicRepo.save(
                KafkaTopicEntity.builder()
                        .cluster(cluster)
                        .topicName(topicName)
                        .description(req.getDescription())
                        .partitions(req.getPartitions())
                        .replicationFactor(req.getReplicationFactor())
                        .enabled(true)
                        .status(KafkaTopicStatus.CREATED)
                        .build()
        );

        return TopicMapper.toDetail(saved);
    }

    // -------------------------------------------------------
    // LIST Topics (DB only)
    // -------------------------------------------------------
    @PreAuthorize("hasAnyRole('KAFKA_ADMIN','KAFKA_DEV','KAFKA_SUPP','KAFKA_TEST')")
    @Transactional(readOnly = true)
    public List<TopicSummary> listTopics(UUID clusterId) {

        if (!clusterRepo.existsById(clusterId)) {
            throw new ResourceNotFoundException("Kafka cluster not found: " + clusterId);
        }

        return topicRepo.findAllByCluster_Id(clusterId)
                .stream()
                .map(TopicMapper::toSummary)
                .toList();
    }

    // -------------------------------------------------------
    // GET Topic Detail (DB only)
    // -------------------------------------------------------
    @PreAuthorize("hasAnyRole('KAFKA_ADMIN','KAFKA_DEV','KAFKA_SUPP','KAFKA_TEST')")
    @Transactional(readOnly = true)
    public TopicDetail getTopic(UUID clusterId, String topicName) {

        if (!clusterRepo.existsById(clusterId)) {
            throw new ResourceNotFoundException("Kafka cluster not found: " + clusterId);
        }

        KafkaTopicEntity entity = topicRepo
                .findByCluster_IdAndTopicName(clusterId, topicName)
                .orElseThrow(() -> new TopicNotFoundException(clusterId, topicName));

        return TopicMapper.toDetail(entity);
    }

    // -------------------------------------------------------
    // UPDATE Topic (Kafka + DB)
    // -------------------------------------------------------
    @PreAuthorize("hasRole('KAFKA_ADMIN')")
    @Transactional
    public TopicDetail updateTopic(UUID clusterId, String topicName, TopicUpdateRequest req) {

        KafkaClusterEntity cluster = clusterRepo.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Kafka cluster not found: " + clusterId));

        if (!cluster.isEnabled()) {
            throw new DomainValidationException("Kafka cluster is disabled: " + cluster.getName());
        }

        KafkaTopicEntity entity = topicRepo.findByCluster_IdAndTopicName(clusterId, topicName)
                .orElseThrow(() -> new TopicNotFoundException(clusterId, topicName));

        try (AdminClient admin = adminFactory.create(cluster.getBootstrapServers())) {

            // ✅ NEW: cache Kafka internal clusterId into DB (safe + non-blocking)
            cacheKafkaClusterIdIfNeeded(admin, cluster);

            // Partitions can only INCREASE in Kafka
            Integer requestedPartitions = req.getPartitions();
            if (requestedPartitions != null) {
                int current = entity.getPartitions() != null ? entity.getPartitions() : 0;

                if (requestedPartitions < current) {
                    throw new DomainValidationException(
                            "Kafka does not allow decreasing partitions. Current=" + current +
                                    ", requested=" + requestedPartitions
                    );
                }

                if (requestedPartitions > current) {
                    admin.createPartitions(Map.of(
                                    topicName, NewPartitions.increaseTo(requestedPartitions)
                            ))
                            .all()
                            .get(8, TimeUnit.SECONDS);

                    entity.setPartitions(requestedPartitions);
                }
            }

            Map<String, String> configs = req.getConfigs();
            if (configs != null && !configs.isEmpty()) {
                applyTopicConfigs(admin, topicName, configs);
            }

        } catch (DomainValidationException e) {
            throw e;
        } catch (Exception e) {
            if (isUnknownTopic(e)) {
                throw new TopicNotFoundException(clusterId, topicName);
            }
            throw new KafkaOperationException(
                    "Kafka topic update failed: " + topicName + " (cluster=" + cluster.getName() + ")",
                    e
            );
        }

        // DB-only updates
        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }
        if (req.getEnabled() != null) {
            entity.setEnabled(req.getEnabled());
        }

        KafkaTopicEntity saved = topicRepo.save(entity);
        return TopicMapper.toDetail(saved);
    }

    // -------------------------------------------------------
    // DELETE Topic (Kafka + DB)
    // -------------------------------------------------------
    @PreAuthorize("hasRole('KAFKA_ADMIN')")
    @Transactional
    public void deleteTopic(UUID clusterId, String topicName) {

        KafkaClusterEntity cluster = clusterRepo.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Kafka cluster not found: " + clusterId));

        if (!cluster.isEnabled()) {
            throw new DomainValidationException("Kafka cluster is disabled: " + cluster.getName());
        }

        KafkaTopicEntity entity = topicRepo.findByCluster_IdAndTopicName(clusterId, topicName)
                .orElseThrow(() -> new TopicNotFoundException(clusterId, topicName));

        try (AdminClient admin = adminFactory.create(cluster.getBootstrapServers())) {

            // ✅ NEW: cache Kafka internal clusterId into DB (safe + non-blocking)
            cacheKafkaClusterIdIfNeeded(admin, cluster);

            admin.deleteTopics(List.of(topicName))
                    .all()
                    .get(8, TimeUnit.SECONDS);

        } catch (Exception e) {
            if (isUnknownTopic(e)) {
                throw new TopicNotFoundException(clusterId, topicName);
            }
            throw new KafkaOperationException(
                    "Kafka topic deletion failed: " + topicName + " (cluster=" + cluster.getName() + ")",
                    e
            );
        }

        topicRepo.delete(entity);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private void cacheKafkaClusterIdIfNeeded(AdminClient admin, KafkaClusterEntity cluster) {
        try {
            String kafkaClusterId = admin.describeCluster()
                    .clusterId()
                    .get(8, TimeUnit.SECONDS);

            if (kafkaClusterId == null || kafkaClusterId.isBlank()) return;

            if (cluster.getKafkaClusterId() == null || !cluster.getKafkaClusterId().equals(kafkaClusterId)) {
                cluster.setKafkaClusterId(kafkaClusterId);
                clusterRepo.save(cluster);
                log.info("Cached kafka_cluster_id={} for cluster name={}", kafkaClusterId, cluster.getName());
            }
        } catch (Exception e) {
            // Do NOT fail topic operations if caching clusterId fails
            log.debug("Unable to fetch/cache Kafka clusterId for cluster name={}", cluster.getName(), e);
        }
    }

    private void applyTopicConfigs(AdminClient admin, String topicName, Map<String, String> configs) throws Exception {

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

        List<AlterConfigOp> ops = new ArrayList<>();
        for (Map.Entry<String, String> e : configs.entrySet()) {
            ops.add(new AlterConfigOp(
                    new ConfigEntry(e.getKey(), e.getValue()),
                    AlterConfigOp.OpType.SET
            ));
        }

        admin.incrementalAlterConfigs(Map.of(resource, ops))
                .all()
                .get(8, TimeUnit.SECONDS);
    }

    private boolean isUnknownTopic(Throwable t) {
        while (t != null) {
            if (t instanceof UnknownTopicOrPartitionException) return true;
            t = t.getCause();
        }
        return false;
    }
}
