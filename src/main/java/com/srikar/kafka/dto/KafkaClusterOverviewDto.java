package com.srikar.kafka.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KafkaClusterOverviewDto {

    // DB-only
    private KafkaClusterMetaDto meta;

    // Live-only
    private KafkaClusterHealthDto health;

}
