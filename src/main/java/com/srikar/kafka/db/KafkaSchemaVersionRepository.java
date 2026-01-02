// src/main/java/com/srikar/kafka/db/KafkaSchemaVersionRepository.java
package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaSchemaVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KafkaSchemaVersionRepository extends JpaRepository<KafkaSchemaVersionEntity, UUID> {

    @Query("""
           select max(v.version)
           from KafkaSchemaVersionEntity v
           where v.subjectId = :subjectId
           """)
    Integer findMaxVersion(UUID subjectId);

    Optional<KafkaSchemaVersionEntity> findBySubjectIdAndVersion(UUID subjectId, int version);

    Optional<KafkaSchemaVersionEntity> findFirstBySubjectIdOrderByVersionDesc(UUID subjectId);

    boolean existsBySubjectIdAndSchemaHash(UUID subjectId, String schemaHash);

    List<KafkaSchemaVersionEntity> findBySubjectIdOrderByVersionDesc(UUID subjectId);

    // ✅ REQUIRED for hard delete (versions first, then subject)
    @Modifying
    @Query("delete from KafkaSchemaVersionEntity v where v.subjectId = :subjectId")
    void deleteBySubjectId(UUID subjectId);
}
