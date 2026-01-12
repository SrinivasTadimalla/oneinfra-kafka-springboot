package com.srikar.kafka.dto.consumer;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerTailCursor {
    private Integer partition;
    private Long offset;
}
