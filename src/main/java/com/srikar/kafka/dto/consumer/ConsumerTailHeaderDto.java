package com.srikar.kafka.dto.consumer;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerTailHeaderDto {
    private String key;

    /** value as UTF-8 best-effort (or base64 if not UTF-8) */
    private String value;
}
