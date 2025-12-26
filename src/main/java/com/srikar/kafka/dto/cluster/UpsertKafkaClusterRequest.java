package com.srikar.kafka.dto.cluster;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertKafkaClusterRequest {
    private String name;              // oneinfra-kafka
    private String mode;              // "KRAFT"
    private String bootstrapServers;  // "ip1:9092,ip2:9092"
    private List<UpsertKafkaNodeRequest> nodes;
}
