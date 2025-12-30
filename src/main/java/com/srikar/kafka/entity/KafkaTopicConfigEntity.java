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
        name = "kafka_topic_configs",
        schema = "iaas_kafka",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_topic_config_topic_key",
                        columnNames = {"topic_id", "config_key"}
                )
        },
        indexes = {
                @Index(name = "ix_topic_configs_topic_id", columnList = "topic_id"),
                @Index(name = "ix_topic_configs_key", columnList = "config_key")
        }
)
public class KafkaTopicConfigEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * FK -> iaas_kafka.kafka_topics(id)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "topic_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_topic_configs_topic")
    )
    private KafkaTopicEntity topic;

    @Column(name = "config_key", nullable = false, length = 300)
    private String configKey;

    @Column(name = "config_value", length = 4000)
    private String configValue;

    /**
     * Where did this value come from?
     * Example:
     * - DB_DEFAULT (you set when creating topic)
     * - KAFKA_LIVE  (fetched from AdminClient)
     */
    @Column(name = "source", nullable = false, length = 30)
    @Builder.Default
    private String source = "DB_DEFAULT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) this.id = UUID.randomUUID();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
