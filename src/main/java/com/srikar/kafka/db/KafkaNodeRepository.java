package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KafkaNodeRepository extends JpaRepository<KafkaNodeEntity, UUID> {

    // Nodes by cluster UUID (navigates via node.cluster.id)
    List<KafkaNodeEntity> findAllByCluster_IdOrderByHostAscPortAsc(UUID clusterId);

    // If you keep nodeId/brokerId in DB
    Optional<KafkaNodeEntity> findByCluster_IdAndNodeId(UUID clusterId, Integer nodeId);

    boolean existsByCluster_IdAndHostAndPort(UUID clusterId, String host, Integer port);
}
