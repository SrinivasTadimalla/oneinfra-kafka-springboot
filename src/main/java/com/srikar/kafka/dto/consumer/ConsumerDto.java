package com.srikar.kafka.dto.consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public final class ConsumerDto {

    private ConsumerDto() {}

    public enum Position {
        EARLIEST,
        LATEST,
        OFFSET,
        TIMESTAMP
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FetchRequest(
            String clusterName,
            String topicName,

            List<Integer> partitions,

            Position position,
            Long offset,
            Long timestampMs,

            Integer maxMessages,
            Integer pollTimeoutMs
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FetchResponse(
            String clusterName,
            String topicName,
            int count,
            List<ConsumerRecordDto> records
    ) {}
}
