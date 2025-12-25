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
                @UniqueConstraint(
                        name = "uk_kafka_cluster_name",
                        columnNames = {"cluster_name"}
                )
        }
)
public class KafkaClusterEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cluster_name", nullable = false, length = 120)
    private String name;

    /**
     * Example values:
     * - KRAFT
     * - ZOOKEEPER (legacy, if ever needed)
     */
    @Column(name = "mode", nullable = false, length = 50)
    private String mode;

    /**
     * Comma-separated bootstrap servers.
     * Used directly by AdminClient.
     */
    @Column(name = "bootstrap_servers", nullable = false, length = 1000)
    private String bootstrapServers;

    /**
     * Permanently control whether this cluster is active in the UI / health probes.
     * Default = true.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /**
     * Optional tag (LAB / DEV / UAT / PROD). Keep nullable until you use it.
     */
    @Column(name = "environment", length = 50)
    private String environment;

    @Column(name = "created_at", nullable = false)
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
    private List<KafkaNodeEntity> nodes = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        // Defensive defaults (in case someone bypasses builder defaults)
        // Note: primitive boolean defaults to false, so force true here if desired.
        // Keep consistent with DB DEFAULT true.
        if (!this.enabled) {
            // If you want to preserve explicit false on insert, remove this block.
            this.enabled = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Convenience helpers */
    public void addNode(KafkaNodeEntity node) {
        nodes.add(node);
        node.setCluster(this);
    }

    public void removeNode(KafkaNodeEntity node) {
        nodes.remove(node);
        node.setCluster(null);
    }
}
