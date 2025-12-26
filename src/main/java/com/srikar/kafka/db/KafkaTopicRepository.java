package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaTopicEntity;
import com.srikar.kafka.enums.KafkaTopicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KafkaTopicRepository extends JpaRepository<KafkaTopicEntity, UUID> {

    // ✅ cluster.id + topicName (unique per cluster)
    boolean existsByCluster_IdAndTopicName(UUID clusterId, String topicName);

    Optional<KafkaTopicEntity> findByCluster_IdAndTopicName(UUID clusterId, String topicName);

    // ✅ list topics under a cluster
    List<KafkaTopicEntity> findAllByCluster_Id(UUID clusterId);

    List<KafkaTopicEntity> findAllByCluster_IdAndEnabledTrue(UUID clusterId);

    // optional filter
    List<KafkaTopicEntity> findAllByCluster_IdAndStatus(UUID clusterId, KafkaTopicStatus status);

    // ✅ For hard delete
    void deleteByCluster_IdAndTopicName(UUID clusterId, String topicName);

    // ✅ Optional helper (topic existence under cluster)
    boolean existsByCluster_Id(UUID clusterId);
}
