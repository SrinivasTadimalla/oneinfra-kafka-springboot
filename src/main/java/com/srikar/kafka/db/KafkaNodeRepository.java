package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KafkaNodeRepository extends JpaRepository<KafkaNodeEntity, UUID> {
    // Not required for upsert because we manage nodes via KafkaClusterEntity.nodes + orphanRemoval=true
}
