package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaConsumerGroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KafkaConsumerGroupRepository extends JpaRepository<KafkaConsumerGroupEntity, UUID> {

    // ✅ Used for "Register" (idempotent: don't create duplicates)
    Optional<KafkaConsumerGroupEntity> findByClusterIdAndGroupId(UUID clusterId, String groupId);

    boolean existsByClusterIdAndGroupId(UUID clusterId, String groupId);

    // ✅ List groups for a cluster (for UI)
    Page<KafkaConsumerGroupEntity> findAllByClusterId(UUID clusterId, Pageable pageable);

    // ✅ Search by groupId (contains) within a cluster
    Page<KafkaConsumerGroupEntity> findAllByClusterIdAndGroupIdContainingIgnoreCase(
            UUID clusterId,
            String groupId,
            Pageable pageable
    );

    // ✅ Optional: search by customer name within a cluster
    Page<KafkaConsumerGroupEntity> findAllByClusterIdAndCustomerNameContainingIgnoreCase(
            UUID clusterId,
            String customerName,
            Pageable pageable
    );
}
