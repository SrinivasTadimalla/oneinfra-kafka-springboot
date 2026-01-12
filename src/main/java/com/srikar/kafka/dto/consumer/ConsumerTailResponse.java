package com.srikar.kafka.dto.consumer;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerTailResponse {

    private String clusterName;
    private String topicName;

    /** total returned in this call */
    private int fetched;

    @Builder.Default
    private List<ConsumerTailRecordDto> records = new ArrayList<>();

    /** cursor to pass back in next poll call */
    private ConsumerTailCursor nextCursor;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
