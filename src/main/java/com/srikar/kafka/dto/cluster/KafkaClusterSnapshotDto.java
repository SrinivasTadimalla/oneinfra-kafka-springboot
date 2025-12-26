package com.srikar.kafka.dto.cluster;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaClusterSnapshotDto {
    private KafkaClusterMetaDto meta;
    private KafkaClusterHealthDto health;
}
