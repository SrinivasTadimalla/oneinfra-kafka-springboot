package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.domain.ClusterActionResult;
import com.srikar.kafka.service.ClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(
        path = "/cluster",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class ClusterController {

    private final ClusterManager clusterManager;

    public ClusterController(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    // ---------- Status (DB read-only) ----------
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status() {

        List<Map<String, Object>> nodes = clusterManager.snapshot(clusterName());

        Map<String, Object> payload = Map.of(
                "cluster", clusterName(),
                "nodes", nodes
        );

        ApiResponse<Map<String, Object>> body = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Cluster status fetched successfully")
                .data(payload)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.ok(body);
    }

    // ---------- Controller actions ----------
    @PostMapping("/controller/start")
    public ResponseEntity<ApiResponse<ClusterActionResult>> controllerStart() {
        log.info("Request to START controller for cluster {}", clusterName());
        return respond(clusterManager.controllerAction("start"));
    }

    @PostMapping("/controller/stop")
    public ResponseEntity<ApiResponse<ClusterActionResult>> controllerStop() {
        log.info("Request to STOP controller for cluster {}", clusterName());
        return respond(clusterManager.controllerAction("stop"));
    }

    @PostMapping("/controller/restart")
    public ResponseEntity<ApiResponse<ClusterActionResult>> controllerRestart() {
        log.info("Request to RESTART controller for cluster {}", clusterName());
        return respond(clusterManager.controllerAction("restart"));
    }

    @GetMapping("/controller/status")
    public ResponseEntity<ApiResponse<ClusterActionResult>> controllerStatus() {
        log.info("Request to get controller STATUS for cluster {}", clusterName());
        return respond(clusterManager.controllerAction("status"));
    }

    // ---------- Broker actions ----------
    @PostMapping("/broker/{brokerId}/start")
    public ResponseEntity<ApiResponse<ClusterActionResult>> brokerStart(@PathVariable short brokerId) {
        log.info("Request to START broker {} in cluster {}", brokerId, clusterName());
        return respond(clusterManager.brokerAction(brokerId, "start"));
    }

    @PostMapping("/broker/{brokerId}/stop")
    public ResponseEntity<ApiResponse<ClusterActionResult>> brokerStop(@PathVariable short brokerId) {
        log.info("Request to STOP broker {} in cluster {}", brokerId, clusterName());
        return respond(clusterManager.brokerAction(brokerId, "stop"));
    }

    @PostMapping("/broker/{brokerId}/restart")
    public ResponseEntity<ApiResponse<ClusterActionResult>> brokerRestart(@PathVariable short brokerId) {
        log.info("Request to RESTART broker {} in cluster {}", brokerId, clusterName());
        return respond(clusterManager.brokerAction(brokerId, "restart"));
    }

    @GetMapping("/broker/{brokerId}/status")
    public ResponseEntity<ApiResponse<ClusterActionResult>> brokerStatus(@PathVariable short brokerId) {
        log.info("Request to get STATUS for broker {} in cluster {}", brokerId, clusterName());
        return respond(clusterManager.brokerAction(brokerId, "status"));
    }

    // ---------- Helpers ----------
    private String clusterName() {
        // Optionally source from properties later
        return "kraft-cluster-1";
    }

    private ResponseEntity<ApiResponse<ClusterActionResult>> respond(ClusterActionResult r) {

        boolean ok = (r != null && r.ok());

        String message = ok
                ? "Cluster action completed successfully"
                : "Cluster action failed";

        ApiResponse<ClusterActionResult> body = ApiResponse.<ClusterActionResult>builder()
                .success(ok)
                .message(message)
                .data(r)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity
                .status(ok ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
