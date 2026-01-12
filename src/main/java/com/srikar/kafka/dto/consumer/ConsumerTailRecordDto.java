package com.srikar.kafka.dto.consumer;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerTailRecordDto {

    private String topic;
    private int partition;
    private long offset;

    /** epoch millis */
    private long timestamp;

    /** optional base64 string */
    private String key;

    /** base64 string (recommended) */
    private String value;

    @Builder.Default
    private List<ConsumerTailHeaderDto> headers = new ArrayList<>();
}
