package com.srikar.kafka.dto.cluster;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)   // ✅ enables toBuilder()
public class KafkaClusterMetaDto {

    // ✅ DB identifier (used by UI for API calls)
    private UUID id;

    private String name;
    private String environment;

    // DB value (display only)
    private String bootstrapServers;

    private boolean enabled;

    // ✅ Kafka internal storage / metadata ID
    // Example: FVPHsj-BSC2rSo0UYt7vmg
    private String kafkaClusterId;

    private Instant updatedAt;
}
