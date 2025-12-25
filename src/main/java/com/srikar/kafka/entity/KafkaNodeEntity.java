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
        name = "kafka_nodes",
        schema = "iaas_kafka",
        indexes = {
                @Index(
                        name = "ix_kafka_nodes_cluster_id",
                        columnList = "cluster_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kafka_node_cluster_host_port",
                        columnNames = {"cluster_id", "host", "port"}
                )
        }
)
public class KafkaNodeEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cluster_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_kafka_nodes_cluster")
    )
    private KafkaClusterEntity cluster;

    /**
     * Optional Kafka node.id / broker.id
     * Can be learned later via AdminClient
     */
    @Column(name = "node_id")
    private Integer nodeId;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    /**
     * Examples:
     * - CONTROLLER
     * - BROKER
     * - BOTH
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * ✅ Renamed from `isVm` -> `vm`
     * Keeps DB column as `is_vm` but generates clean Lombok accessors:
     *   boolean isVm()
     *   void setVm(boolean)
     */
    @Column(name = "is_vm", nullable = false)
    private boolean vm;

    @Column(name = "vm_name", length = 120)
    private String vmName;

    /**
     * Last time this node was observed healthy / reachable
     */
    @Column(name = "observed_at")
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
}
