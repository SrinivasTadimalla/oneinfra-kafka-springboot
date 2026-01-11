package com.srikar.kafka.dto.consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsumerRecordDto(
        int partition,
        long offset,
        long timestamp,

        // base64 (safe for binary)
        String key,
        String value,

        // "k=v; k2=v2" (best effort)
        String headers,

        Integer sizeBytes
) {}
