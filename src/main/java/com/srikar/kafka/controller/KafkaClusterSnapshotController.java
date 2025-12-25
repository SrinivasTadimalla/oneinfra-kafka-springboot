package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.KafkaClusterOverviewDto;
import com.srikar.kafka.dto.KafkaClusterSnapshotDto;
import com.srikar.kafka.dto.KafkaClusterSummaryDto;
import com.srikar.kafka.service.KafkaClusterSnapshotService;
import com.srikar.kafka.utilities.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kafka/clusters")
@RequiredArgsConstructor
public class KafkaClusterSnapshotController {

    private final KafkaClusterSnapshotService snapshotService;

    // ✅ List clusters from DB (metadata only)
    @GetMapping
    public ResponseEntity<ApiResponse<List<KafkaClusterSummaryDto>>> listClusters() {
        List<KafkaClusterSummaryDto> clusters = snapshotService.listClusters();
        return ResponseEntity.ok(ApiResponses.ok("Kafka clusters fetched successfully", clusters));
    }

    // Existing: single cluster snapshot (DB + live probe)
    @GetMapping("/{name}/snapshot")
    public ResponseEntity<ApiResponse<KafkaClusterSnapshotDto>> snapshot(@PathVariable("name") String name) {
        KafkaClusterSnapshotDto dto = snapshotService.snapshotByName(name);
        return ResponseEntity.ok(ApiResponses.ok("Kafka cluster snapshot fetched successfully", dto));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<KafkaClusterOverviewDto>>> overview() {
        var dtos = snapshotService.listClustersWithHealth();
        return ResponseEntity.ok(ApiResponses.ok("Kafka clusters overview fetched successfully", dtos));
    }

}
