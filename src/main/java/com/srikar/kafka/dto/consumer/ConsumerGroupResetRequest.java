package com.srikar.kafka.dto.consumer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Reset offsets for a consumer group.
 *
 * Current supported reset strategy:
 * - timestamp (resetOffsetsTimestamp)
 *
 * Future-proof:
 * - you can add resetType=EARLIEST/LATEST/OFFSET later without creating new DTOs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupResetRequest {

    @NotNull
    private UUID clusterId;

    @NotBlank
    private String groupId;

    /**
     * Reset offsets to the first offset whose record timestamp >= this value.
     * Kafka resolves per-partition via OffsetsForTimes.
     */
    @NotNull
    private Instant timestamp;

    /**
     * If Kafka returns "no offset for timestamp" for some partitions
     * (e.g., timestamp too old/new), fallback those partitions to earliest.
     */
    @Builder.Default
    private boolean fallbackToEarliest = true;

    /**
     * If true: compute what would change, but DO NOT alter offsets.
     */
    @Builder.Default
    private boolean dryRun = false;

    /**
     * Optional: safety guard.
     * If true, only allow reset when group is EMPTY (no active members).
     * (We can enforce this in service later.)
     */
    @Builder.Default
    private boolean requireInactiveGroup = true;
}
