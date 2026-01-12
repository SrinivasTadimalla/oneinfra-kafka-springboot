package com.srikar.kafka.dto.consumer;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerTailCursor {

    // Key - Partition Number
    // Value - Offset Number
    private Map<Integer, Long> offsetsByPartition;

}
