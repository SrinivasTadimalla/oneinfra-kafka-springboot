package com.srikar.kafka.entity;

import com.srikar.kafka.enums.KafkaTopicStatus;
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
        name = "kafka_topics",
        schema = "iaas_kafka",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_topic_per_cluster", columnNames = {"cluster_id", "topic_name"})
        },
        indexes = {
                @Index(name = "idx_kafka_topics_cluster_id", columnList = "cluster_id"),
                @Index(name = "idx_kafka_topics_topic_name", columnList = "topic_name")
        }
)
public class KafkaTopicEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Read-only FK column for safe mapping/search without forcing LAZY cluster init.
     * ✅ No DB change. Uses existing column "cluster_id".
     */
    @Column(name = "cluster_id", nullable = false, insertable = false, updatable = false)
    private UUID clusterId;

    // FK -> iaas_kafka.kafka_clusters(id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cluster_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_kafka_topics_cluster")
    )
    private KafkaClusterEntity cluster;

    @Column(name = "topic_name", nullable = false, length = 255)
    private String topicName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "partitions", nullable = false)
    private Integer partitions;

    @Column(name = "replication_factor", nullable = false)
    private Short replicationFactor;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private KafkaTopicStatus status = KafkaTopicStatus.CREATED;

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
