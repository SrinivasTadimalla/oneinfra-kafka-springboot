package com.srikar.kafka.dto.topic;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicCreateRequest {

    @NotNull
    private UUID clusterId;

    @NotBlank
    @Size(min = 1, max = 249)
    private String topicName;

    @NotNull
    @Min(1)
    @Max(1000)
    private Integer partitions;

    @NotNull
    @Min(1)
    @Max(10)
    private Short replicationFactor;

    @Size(max = 500)
    private String description;

    // Optional topic-level configs (retention.ms, cleanup.policy, etc.)
    private Map<String, String> configs;
}
