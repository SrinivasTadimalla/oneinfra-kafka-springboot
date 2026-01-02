package com.srikar.kafka.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "kafka_schema_versions",
        schema = "iaas_kafka",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_schema_version_per_subject",
                        columnNames = {"subject_id", "version"}
                )
        },
        indexes = {
                @Index(name = "ix_schema_versions_subject_id", columnList = "subject_id"),
                @Index(name = "ix_schema_versions_version", columnList = "version")
        }
)
public class KafkaSchemaVersionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Read-only FK */
    @Column(name = "subject_id", nullable = false, insertable = false, updatable = false)
    private UUID subjectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "subject_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schema_versions_subject")
    )
    private KafkaSchemaSubjectEntity subject;

    /** 1,2,3,... */
    @Column(name = "version", nullable = false)
    private Integer version;

    /** Canonical form */
    @Column(name = "canonical_text", columnDefinition = "text", nullable = false)
    private String schemaCanonical;

    /** Raw schema text */
    @Column(name = "schema_text", columnDefinition = "text", nullable = false)
    private String schemaRaw;

    /** SHA-256 fingerprint */
    @Column(name = "fingerprint_sha256", length = 128, nullable = false)
    private String schemaHash;

    /** ACTIVE / DEPRECATED / FAILED */
    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
