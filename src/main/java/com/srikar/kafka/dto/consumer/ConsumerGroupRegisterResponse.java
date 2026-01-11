package com.srikar.kafka.dto.consumer;

import lombok.*;

        import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupRegisterResponse {
    private UUID id;
    private UUID clusterId;
    private String groupId;
    private String description;
    private String customerName;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
