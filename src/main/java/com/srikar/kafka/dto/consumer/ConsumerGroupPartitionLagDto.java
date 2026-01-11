package com.srikar.kafka.dto.consumer;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupPartitionLagDto {
    private String topic;
    private int partition;
    private long committedOffset;
    private long endOffset;
    private long lag;
}
