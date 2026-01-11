package com.srikar.kafka.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "kafka_consumer_groups",
        schema = "iaas_kafka",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_group_cluster", columnNames = {"cluster_id", "group_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaConsumerGroupEntity {

    @Id
    @GeneratedValue
    private UUID id;

    /** Which Kafka cluster this group belongs to */
    @Column(name = "cluster_id", nullable = false)
    private UUID clusterId;

    /** Kafka group.id */
    @Column(name = "group_id", nullable = false, length = 200)
    private String groupId;

    /** Friendly description for UI */
    @Column(name = "description", length = 500)
    private String description;

    /** Which customer / application owns this group */
    @Column(name = "customer_name", length = 200)
    private String customerName;

    /** Whether OneInfra allows resets / management on this group */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** Who registered it */
    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
        this.enabled = true;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

}
