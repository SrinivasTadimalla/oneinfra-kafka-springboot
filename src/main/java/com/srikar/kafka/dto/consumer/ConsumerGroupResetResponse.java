package com.srikar.kafka.dto.consumer;

import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Result of a consumer group reset (or dry-run).
 * UI can render partition-by-partition changes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupResetResponse {

    private UUID clusterId;
    private String groupId;

    /** the timestamp used for the reset */
    private Instant timestamp;

    /** whether this was only a dry-run (no offsets altered) */
    @Builder.Default
    private boolean dryRun = false;

    /** whether the reset actually applied */
    @Builder.Default
    private boolean applied = false;

    /** high-level summary */
    @Builder.Default
    private int partitionsAffected = 0;

    @Builder.Default
    private int partitionsUnchanged = 0;

    /** warnings you want to show in UI (ex: "group was active") */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /** per partition results */
    @Builder.Default
    private List<PartitionChange> changes = new ArrayList<>();

    /** any error message if you want to return a structured failure */
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartitionChange {
        private String topic;
        private int partition;

        /** committed offset BEFORE reset (if exists) */
        private long beforeOffset;

        /** committed offset AFTER reset (or would-be offset if dryRun) */
        private long afterOffset;

        /** Kafka resolved offset timestamp (if known); optional */
        private Long resolvedOffsetTimestamp;

        /** how much you moved the pointer (positive means rewound/back in time) */
        private long delta;

        /** whether this partition changed */
        private boolean changed;

        /** optional note (ex: "no offset for timestamp -> used earliest") */
        private String note;
    }
}
