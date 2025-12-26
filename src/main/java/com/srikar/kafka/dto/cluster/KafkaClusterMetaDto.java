package com.srikar.kafka.dto.cluster;

import lombok.*;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KafkaClusterMetaDto {
    private String name;
    private String environment;
    private String bootstrapServers;
    private boolean enabled;
    private Instant updatedAt;
}
