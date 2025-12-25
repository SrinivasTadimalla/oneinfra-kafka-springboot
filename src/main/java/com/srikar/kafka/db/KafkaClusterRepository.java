package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaClusterEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KafkaClusterRepository extends JpaRepository<KafkaClusterEntity, UUID> {

    List<KafkaClusterEntity> findAllByOrderByNameAsc();

    Optional<KafkaClusterEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "nodes")
    Optional<KafkaClusterEntity> findWithNodesByNameIgnoreCase(String name);
}
