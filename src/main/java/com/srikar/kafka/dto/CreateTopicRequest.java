package com.srikar.kafka.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateTopicRequest {

    @NotBlank
    private String topicName;

    @Min(1)
    private int partitions = 1;

    @Min(1)
    private short replicationFactor = 1;
}

