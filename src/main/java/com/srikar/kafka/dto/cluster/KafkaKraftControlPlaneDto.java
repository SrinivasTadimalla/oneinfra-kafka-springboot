// src/main/java/com/srikar/kafka/dto/cluster/KafkaKraftControlPlaneDto.java
package com.srikar.kafka.dto.cluster;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaKraftControlPlaneDto {

    private String node;             // "oneinfra-host" (label)
    private String role;             // "CONTROLLER"
    private String listener;         // "192.168.66.1:9093" (controller listener)
    private Integer quorumNodeId;    // 0
    private String mode;             // "SINGLE_NODE_QUORUM"
    private String status;           // "UP" | "DOWN" | "UNKNOWN"
    private String observedAt;       // ISO string
    private String source;           // "JMX"
    private String error;            // nullable
}
