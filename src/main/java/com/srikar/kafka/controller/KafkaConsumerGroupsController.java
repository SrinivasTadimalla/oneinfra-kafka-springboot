package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.consumer.ConsumerGroupDetailDto;
import com.srikar.kafka.dto.consumer.ConsumerGroupSummaryDto;
import com.srikar.kafka.service.KafkaConsumerGroupsService;
import com.srikar.kafka.utilities.ApiResponses;
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
}
