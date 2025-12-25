package com.srikar.kafka.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertKafkaNodeRequest {
    private Integer nodeId;    // optional
    private String host;
    private Integer port;
    private String role;       // "CONTROLLER" | "BROKER" | "BOTH"
    private boolean isVm;
    private String vmName;     // optional
}
