package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaClusterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KafkaClusterRepository extends JpaRepository<KafkaClusterEntity, Long> {

    // Used by: listClustersWithHealth()
    List<KafkaClusterEntity> findAllByOrderByNameAsc();

    // Used by: snapshotByName(name)
    Optional<KafkaClusterEntity> findByName(String name);

    // Optional (nice to have for validations)
    boolean existsByName(String name);
}
