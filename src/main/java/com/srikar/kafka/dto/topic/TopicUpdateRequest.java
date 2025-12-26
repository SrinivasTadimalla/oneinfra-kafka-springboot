package com.srikar.kafka.dto.topic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicUpdateRequest {

    /**
     * Optional: increase partitions (Kafka only allows increasing).
     * If null => do not change partitions.
     */
    @Min(1)
    @Max(10000)
    private Integer partitions;

    /**
     * Optional: update configs like retention.ms, cleanup.policy, etc.
     * If null or empty => no config changes.
     */
    private Map<String, String> configs;

    /**
     * Optional: update description in DB only
     */
    private String description;

    /**
     * Optional: enable/disable in DB only (does NOT affect Kafka)
     */
    private Boolean enabled;
}
