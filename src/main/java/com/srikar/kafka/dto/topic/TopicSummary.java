package com.srikar.kafka.dto.topic;

import com.srikar.kafka.enums.KafkaTopicStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicSummary {

    private UUID id;
    private UUID clusterId;

    private String topicName;
    private Integer partitions;
    private Short replicationFactor;

    private boolean enabled;
    private KafkaTopicStatus status;

    private Instant updatedAt;
}
