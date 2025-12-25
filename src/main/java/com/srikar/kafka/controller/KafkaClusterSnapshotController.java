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

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<KafkaClusterOverviewDto>>> overview() {
        var list = snapshotService.listClustersWithHealth();
        return ResponseEntity.ok(ApiResponses.ok("Kafka clusters overview fetched successfully", list));
    }

}
