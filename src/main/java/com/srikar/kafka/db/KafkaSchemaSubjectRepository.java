package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaSchemaSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KafkaSchemaSubjectRepository extends JpaRepository<KafkaSchemaSubjectEntity, UUID> {

    Optional<KafkaSchemaSubjectEntity> findByClusterIdAndSubject(UUID clusterId, String subject);

    List<KafkaSchemaSubjectEntity> findAllByClusterIdOrderBySubjectAsc(UUID clusterId);

    boolean existsByClusterIdAndSubject(UUID clusterId, String subject);
}
