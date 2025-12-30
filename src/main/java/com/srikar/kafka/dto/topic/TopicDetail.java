package com.srikar.kafka.dto.topic;

import com.srikar.kafka.enums.KafkaTopicStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDetail {

    private UUID id;
    private UUID clusterId;

    private String topicName;
    private String description;          // ✅ ADD THIS
    private Integer partitions;
    private Short replicationFactor;

    private boolean enabled;
    private KafkaTopicStatus status;

    // Optional (Kafka-side failure info)
    private String lastError;

    private Map<String, String> configs;

    private Instant createdAt;
    private Instant updatedAt;
}
