package com.srikar.kafka.dto.topic;

import com.srikar.kafka.enums.KafkaTopicStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicUpdateRequest {

    @Min(1)
    @Max(10000)
    private Integer partitions;

    private Map<String, String> configs;

    private String description;

    private Boolean enabled;

    /**
     * Optional: update status in DB (does NOT affect Kafka).
     * If null => no status change.
     */
    private KafkaTopicStatus status;

}
