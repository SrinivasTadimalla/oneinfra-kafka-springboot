package com.srikar.kafka.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaClusterHealthDto {
    private String status;        // UP / DOWN
    private String clusterId;     // from Kafka
    private String controller;    // host:port
    private Integer brokerCount;
    private List<String> brokers; // host:port
    private Instant observedAt;
    private String error;
}
