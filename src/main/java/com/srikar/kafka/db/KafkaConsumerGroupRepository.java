package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaConsumerGroupEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KafkaConsumerGroupRepository
        extends JpaRepository<KafkaConsumerGroupEntity, UUID> {

    // =====================================================
    // Registration & idempotency
    // =====================================================

    Optional<KafkaConsumerGroupEntity> findByClusterIdAndGroupId(
            UUID clusterId,
            String groupId
    );

    boolean existsByClusterIdAndGroupId(
            UUID clusterId,
            String groupId
    );

    // =====================================================
    // UI listing
    // =====================================================

    Page<KafkaConsumerGroupEntity> findAllByClusterId(
            UUID clusterId,
            Pageable pageable
    );

    // =====================================================
    // Search (UI filters)
    // =====================================================

    Page<KafkaConsumerGroupEntity> findAllByClusterIdAndGroupIdContainingIgnoreCase(
            UUID clusterId,
            String groupId,
            Pageable pageable
    );

    Page<KafkaConsumerGroupEntity> findAllByClusterIdAndCustomerNameContainingIgnoreCase(
            UUID clusterId,
            String customerName,
            Pageable pageable
    );
}
