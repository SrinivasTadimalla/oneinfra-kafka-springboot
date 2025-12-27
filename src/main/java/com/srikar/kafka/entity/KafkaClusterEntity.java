package com.srikar.kafka.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "kafka_clusters",
        schema = "iaas_kafka",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_kafka_cluster_name", columnNames = {"cluster_name"})
        }
)
public class KafkaClusterEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cluster_name", nullable = false, length = 120)
    private String name;

    @Column(name = "mode", nullable = false, length = 50)
    private String mode;

    @Column(name = "bootstrap_servers", nullable = false, length = 1000)
    private String bootstrapServers;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "environment", length = 50)
    private String environment;

    // ✅ NEW: Kafka internal Cluster ID (stored in DB column kafka_cluster_id)
    // Example value: "FVPHsj-BSC2rSo0UYt7vmg"
    @Column(name = "kafka_cluster_id", length = 64)
    private String kafkaClusterId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "cluster",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<KafkaTopicEntity> topics = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addTopic(KafkaTopicEntity topic) {
        topics.add(topic);
        topic.setCluster(this);
    }

    public void removeTopic(KafkaTopicEntity topic) {
        topics.remove(topic);
        topic.setCluster(null);
    }
}
