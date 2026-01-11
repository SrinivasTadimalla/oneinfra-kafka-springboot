package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.consumer.ConsumerGroupDetailDto;
import com.srikar.kafka.dto.consumer.ConsumerGroupResetRequest;
import com.srikar.kafka.dto.consumer.ConsumerGroupResetResponse;
import com.srikar.kafka.dto.consumer.ConsumerGroupSummaryDto;
import com.srikar.kafka.service.KafkaConsumerGroupsService;
import com.srikar.kafka.utilities.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        path = "/api/kafka/consumer-groups",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class KafkaConsumerGroupsController {

    private final KafkaConsumerGroupsService service;

    /**
     * UI: List consumer groups for a cluster
     * GET /api/kafka/consumer-groups?clusterId=...
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsumerGroupSummaryDto>>> list(
            @RequestParam UUID clusterId
    ) {
        List<ConsumerGroupSummaryDto> result = service.listGroups(clusterId);

        return ResponseEntity.ok(
                ApiResponses.ok("Consumer groups loaded successfully", result)
        );
    }

    /**
     * UI: Consumer group details (members + partition lag)
     * GET /api/kafka/consumer-groups/{groupId}?clusterId=...
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<ConsumerGroupDetailDto>> detail(
            @PathVariable String groupId,
            @RequestParam UUID clusterId
    ) {
        ConsumerGroupDetailDto result = service.getGroupDetail(clusterId, groupId);

        return ResponseEntity.ok(
                ApiResponses.ok("Consumer group details loaded successfully", result)
        );
    }

    /**
     * ✅ Reset offsets by timestamp (supports dryRun)
     * POST /api/kafka/consumer-groups/reset/timestamp
     *
     * Body:
     * {
     *   "clusterId": "...",
     *   "groupId": "payments-service",
     *   "timestamp": "2026-01-11T10:30:00Z",
     *   "fallbackToEarliest": true,
     *   "dryRun": true,
     *   "requireInactiveGroup": true
     * }
     */
    @PostMapping(
            path = "/reset/timestamp",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ConsumerGroupResetResponse>> resetByTimestamp(
            @Valid @RequestBody ConsumerGroupResetRequest request
    ) {
        ConsumerGroupResetResponse result = service.resetOffsetsByTimestamp(request);

        // If service returned an error string, still return 200 (UI can render error).
        // If you prefer 4xx/5xx later, we can add that.
        String msg = (result.getError() == null || result.getError().isBlank())
                ? (request.isDryRun()
                ? "Dry-run completed (no offsets altered)"
                : "Offsets reset completed")
                : "Offset reset failed";

        return ResponseEntity.ok(ApiResponses.ok(msg, result));
    }
}
