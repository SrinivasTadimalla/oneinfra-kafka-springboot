package com.srikar.kafka.dto;

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
