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
        name = "kafka_schema_subjects",
        schema = "iaas_kafka",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_schema_subject_per_cluster",
                        columnNames = {"cluster_id", "subject"}
                )
        },
        indexes = {
                @Index(name = "ix_schema_subjects_cluster_id", columnList = "cluster_id"),
                @Index(name = "ix_schema_subjects_subject", columnList = "subject")
        }
)
public class KafkaSchemaSubjectEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /** Read-only FK column (same pattern you used in KafkaTopicEntity) */
    @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false)
    private UUID clusterId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cluster_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schema_subjects_cluster")
    )
    private KafkaClusterEntity cluster;

    /** Example: "oneinfra-schema-value" or "payments.txn.avro" */
    @Column(name = "subject_name", nullable = false, length = 255)
    private String subject;

    /** AVRO / PROTOBUF / JSON_SCHEMA */
    @Column(name = "schema_type", nullable = false, length = 32)
    private String schemaType;

    /**
     * Compatibility policy for this subject:
     * BACKWARD / FORWARD / FULL / NONE (start with BACKWARD or FULL)
     */
    @Column(name = "compatibility", nullable = false, length = 32)
    private String compatibility;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
