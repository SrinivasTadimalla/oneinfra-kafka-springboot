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
import com.srikar.kafka.utilities.TopicKafkaDelta;
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

            cacheKafkaClusterIdIfNeeded(admin, cluster);

            NewTopic newTopic = new NewTopic(
                    topicName,
                    req.getPartitions(),
                    req.getReplicationFactor()
            );

            // 1) Create topic
            admin.createTopics(List.of(newTopic))
                    .all()
                    .get(8, TimeUnit.SECONDS);

            // 2) Apply configs (schema-topic defaults + request configs)
            Map<String, String> finalConfigs = buildCreateConfigs(req);

            if (!finalConfigs.isEmpty()) {
                applyTopicConfigs(admin, topicName, finalConfigs);
            }

        } catch (Exception e) {
            throw new KafkaOperationException(
                    "Kafka topic creation failed: " + topicName +
                            " (cluster=" + cluster.getName() + ")",
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
    // GET Topic Detail + Kafka Configs
    // -------------------------------------------------------
    @PreAuthorize("hasAnyRole('KAFKA_ADMIN','KAFKA_DEV','KAFKA_SUPP','KAFKA_TEST')")
    @Transactional(readOnly = true)
    public TopicDetail getTopicWithConfigs(UUID clusterId, String topicName) {

        KafkaClusterEntity cluster = clusterRepo.findById(clusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Kafka cluster not found: " + clusterId));

        KafkaTopicEntity entity = topicRepo
                .findByCluster_IdAndTopicName(clusterId, topicName)
                .orElseThrow(() -> new TopicNotFoundException(clusterId, topicName));

        TopicDetail detail = TopicMapper.toDetail(entity);

        try (AdminClient admin = adminFactory.create(cluster.getBootstrapServers())) {

            cacheKafkaClusterIdIfNeeded(admin, cluster);

            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

            Map<ConfigResource, Config> result =
                    admin.describeConfigs(List.of(resource))
                            .all()
                            .get(8, TimeUnit.SECONDS);

            Config config = result.get(resource);

            Map<String, String> configs = new HashMap<>();
            for (ConfigEntry e : config.entries()) {
                configs.put(e.name(), e.value());
            }

            detail.setConfigs(configs);

        } catch (Exception e) {
            detail.setLastError("Unable to fetch Kafka topic configs");
            log.debug("Failed to fetch configs for topic={}", topicName, e);
        }

        return detail;
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

        TopicKafkaDelta delta;

        try (AdminClient admin = adminFactory.create(cluster.getBootstrapServers())) {

            cacheKafkaClusterIdIfNeeded(admin, cluster);

            // 1) Compute delta (Kafka + DB)
            delta = computeTopicDelta(entity, req, admin, topicName);

            // 2) If nothing changes, do nothing (no Kafka, no DB save)
            if (!delta.hasAnyChange()) {
                return TopicMapper.toDetail(entity);
            }

            // 3) Apply Kafka changes only if needed
            if (delta.isKafkaChange()) {

                if (delta.getPartitionsIncreaseTo() != null) {
                    admin.createPartitions(Map.of(
                                    topicName,
                                    NewPartitions.increaseTo(delta.getPartitionsIncreaseTo())
                            ))
                            .all()
                            .get(8, TimeUnit.SECONDS);

                    entity.setPartitions(delta.getPartitionsIncreaseTo());
                }

                if (delta.getConfigDelta() != null && !delta.getConfigDelta().isEmpty()) {
                    applyTopicConfigs(admin, topicName, delta.getConfigDelta());
                }
            }

        } catch (DomainValidationException e) {
            throw e;
        } catch (Exception e) {
            if (isUnknownTopic(e)) {
                throw new TopicNotFoundException(clusterId, topicName);
            }
            throw new KafkaOperationException(
                    "Kafka topic update failed: " + topicName +
                            " (cluster=" + cluster.getName() + ")",
                    e
            );
        }

        // 4) Apply DB-only deltas (only if different)
        if (delta.isDbChange()) {
            if (delta.getDescription() != null) entity.setDescription(delta.getDescription());
            if (delta.getEnabled() != null) entity.setEnabled(delta.getEnabled());
            if (delta.getStatus() != null) entity.setStatus(delta.getStatus());
        }

        // 5) Save only if any change happened (Kafka or DB)
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

            cacheKafkaClusterIdIfNeeded(admin, cluster);

            admin.deleteTopics(List.of(topicName))
                    .all()
                    .get(8, TimeUnit.SECONDS);

        } catch (Exception e) {
            if (isUnknownTopic(e)) {
                throw new TopicNotFoundException(clusterId, topicName);
            }
            throw new KafkaOperationException(
                    "Kafka topic deletion failed: " + topicName +
                            " (cluster=" + cluster.getName() + ")",
                    e
            );
        }

        topicRepo.delete(entity);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    /**
     * During CREATE:
     * - If topicName == oneinfra_schemas -> enforce cleanup.policy=compact
     * - Merge request configs (if any)
     */
    private Map<String, String> buildCreateConfigs(TopicCreateRequest req) {

        String topicName = req.getTopicName();
        boolean isSchemaTopic = "oneinfra_schemas".equalsIgnoreCase(topicName);

        Map<String, String> finalConfigs = new HashMap<>();

        // 1) Schema topic defaults
        if (isSchemaTopic) {
            finalConfigs.put("cleanup.policy", "compact");
        }

        // 2) Merge user configs
        if (req.getConfigs() != null && !req.getConfigs().isEmpty()) {
            finalConfigs.putAll(req.getConfigs());
        }

        // 3) Enforce compact for schema topic even if user passed delete
        if (isSchemaTopic) {
            finalConfigs.put("cleanup.policy", "compact");
        }

        return finalConfigs;
    }

    private void cacheKafkaClusterIdIfNeeded(AdminClient admin, KafkaClusterEntity cluster) {
        try {
            String kafkaClusterId = admin.describeCluster()
                    .clusterId()
                    .get(8, TimeUnit.SECONDS);

            if (kafkaClusterId == null || kafkaClusterId.isBlank()) return;

            if (cluster.getKafkaClusterId() == null ||
                    !cluster.getKafkaClusterId().equals(kafkaClusterId)) {

                cluster.setKafkaClusterId(kafkaClusterId);
                clusterRepo.save(cluster);

                log.info(
                        "Cached kafka_cluster_id={} for cluster name={}",
                        kafkaClusterId,
                        cluster.getName()
                );
            }
        } catch (Exception e) {
            log.debug(
                    "Unable to fetch/cache Kafka clusterId for cluster name={}",
                    cluster.getName(),
                    e
            );
        }
    }

    private void applyTopicConfigs(
            AdminClient admin,
            String topicName,
            Map<String, String> configs
    ) throws Exception {

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

    private Map<String, String> fetchCurrentTopicConfigs(
            AdminClient admin,
            String topicName,
            Set<String> keys
    ) throws Exception {

        if (keys == null || keys.isEmpty()) return Map.of();

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

        Map<ConfigResource, Config> result =
                admin.describeConfigs(List.of(resource))
                        .all()
                        .get(8, TimeUnit.SECONDS);

        Config config = result.get(resource);

        Map<String, String> current = new HashMap<>();
        for (ConfigEntry e : config.entries()) {
            if (keys.contains(e.name())) {
                current.put(e.name(), e.value());
            }
        }
        return current;
    }

    private TopicKafkaDelta computeTopicDelta(
            KafkaTopicEntity entity,
            TopicUpdateRequest req,
            AdminClient admin,
            String topicName) throws Exception {

        // ----------------------------
        // Kafka delta: partitions
        // ----------------------------
        Integer partitionsIncreaseTo = null;

        Integer requestedPartitions = req.getPartitions();
        if (requestedPartitions != null) {
            int current = entity.getPartitions() != null ? entity.getPartitions() : 0;

            if (requestedPartitions < current) {
                throw new DomainValidationException(
                        "Kafka does not allow decreasing partitions. Current=" +
                                current + ", requested=" + requestedPartitions
                );
            }

            if (requestedPartitions > current) {
                partitionsIncreaseTo = requestedPartitions;
            }
        }

        // ----------------------------
        // Kafka delta: configs (only apply changed keys)
        // ----------------------------
        Map<String, String> configDelta = new HashMap<>();
        Map<String, String> requestedConfigs = req.getConfigs();

        if (requestedConfigs != null && !requestedConfigs.isEmpty()) {
            Map<String, String> currentKafka =
                    fetchCurrentTopicConfigs(admin, topicName, requestedConfigs.keySet());

            for (var entry : requestedConfigs.entrySet()) {
                String key = entry.getKey();
                String requestedVal = entry.getValue();
                String currentVal = currentKafka.get(key);

                if (!Objects.equals(requestedVal, currentVal)) {
                    configDelta.put(key, requestedVal);
                }
            }
        }

        boolean kafkaChange = (partitionsIncreaseTo != null) || !configDelta.isEmpty();

        // ----------------------------
        // DB delta: description/enabled/status
        // (only mark dbChange if value is different)
        // ----------------------------
        boolean descChanged = (req.getDescription() != null)
                && !Objects.equals(req.getDescription(), entity.getDescription());

        boolean enabledChanged = (req.getEnabled() != null)
                && !Objects.equals(req.getEnabled(), entity.isEnabled());

        boolean statusChanged = (req.getStatus() != null)
                && !Objects.equals(req.getStatus(), entity.getStatus());

        boolean dbChange = descChanged || enabledChanged || statusChanged;

        return TopicKafkaDelta.builder()
                .kafkaChange(kafkaChange)
                .partitionsIncreaseTo(partitionsIncreaseTo)
                .configDelta(configDelta)
                .dbChange(dbChange)
                .description(descChanged ? req.getDescription() : null)
                .enabled(enabledChanged ? req.getEnabled() : null)
                .status(statusChanged ? req.getStatus() : null)
                .build();
    }


}
