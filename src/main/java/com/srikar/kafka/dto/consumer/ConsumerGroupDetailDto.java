package com.srikar.kafka.dto.consumer;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupDetailDto {
    private String groupId;
    private String state;
    private int membersCount;
    private List<String> memberClientIds;

    private int topicsCount;
    private long totalLag;

    private List<ConsumerGroupPartitionLagDto> partitions; // per topic-partition lag
}
