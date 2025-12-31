package com.srikar.kafka.dto.cluster;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class KafkaClusterOverviewDto {

    // DB-only
    private KafkaClusterMetaDto meta;

    // Live-only
    private KafkaClusterHealthDto health;

    // add:
    private KafkaKraftControlPlaneDto controlPlane;

}
