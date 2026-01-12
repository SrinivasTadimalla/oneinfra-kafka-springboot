package com.srikar.kafka.dto.consumer;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerTailRequest {

    /** Keep string to work with your KafkaBootstrapResolver.resolve(clusterName) */
    private String clusterName;

    private String topicName;

    /** optional: poll timeout per call */
    @Builder.Default
    private Integer pollTimeoutMs = 1000;

    /** optional: max messages returned per call */
    @Builder.Default
    private Integer maxMessages = 50;

    /** optional: restrict partitions. null/empty => all partitions */
    private List<Integer> partitions;

    /** cursor from previous call (stateless tail) */
    private ConsumerTailCursor lastSeen;

    /** optional: include headers */
    @Builder.Default
    private boolean includeHeaders = true;

    /** optional: include key */
    @Builder.Default
    private boolean includeKey = true;
}
