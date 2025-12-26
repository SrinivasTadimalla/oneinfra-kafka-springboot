package com.srikar.kafka.dto.cluster;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaNodeMetaDto {
    private UUID id;
    private Integer nodeId;
    private String host;
    private Integer port;
    private String role;
    private boolean isVm;
    private String vmName;
    private Instant observedAt;
}
