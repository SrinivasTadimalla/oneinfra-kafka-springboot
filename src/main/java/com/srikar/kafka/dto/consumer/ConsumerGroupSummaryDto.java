package com.srikar.kafka.dto.consumer;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupSummaryDto {
    private String groupId;
    private String state;        // STABLE / EMPTY / DEAD / ...
    private int membersCount;
    private int topicsCount;
    private long totalLag;
}
