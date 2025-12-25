package com.srikar.kafka.dto;

import lombok.*;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KafkaClusterSummaryDto {
    private String name;
    private String environment;     // dev/prod/lab etc (if you store it)
    private String bootstrapServers; // if you store it (or hide if sensitive)
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    // Optional quick aggregates if you want:
    private Integer brokerCount;
    private Integer controllerCount;
}
