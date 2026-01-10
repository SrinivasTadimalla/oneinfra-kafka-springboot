package com.srikar.kafka.dto.consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public final class ConsumerDto {

    private ConsumerDto() {}

    // ----------------------------
    // Start position (seek mode)
    // ----------------------------
    public enum Position {
        EARLIEST,
        LATEST,
        OFFSET,
        TIMESTAMP
    }

    // ----------------------------
    // Fetch Request
    // ----------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FetchRequest(
            String clusterName,
            String topic,

            // optional: read only specific partitions; null/empty => all partitions
            List<Integer> partitions,

            // how to seek before reading
            Position position,

            // used when position == OFFSET
            Long offset,

            // used when position == TIMESTAMP
            Long timestampMs,

            // limits
            Integer maxMessages,

            // poll duration
            Integer pollTimeoutMs
    ) {}

    // ----------------------------
    // Fetch Response
    // ----------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FetchResponse(
            String clusterName,
            String topic,
            int count,
            List<ConsumerRecordDto> records
    ) {}
}
