package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.domain.TopicActionResult;
import com.srikar.kafka.dto.CreateTopicRequest;
import com.srikar.kafka.dto.DeleteTopicRequest;
import com.srikar.kafka.dto.TopicWithListResponse;
import com.srikar.kafka.service.KafkaTopicService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping(
        path = "/topics",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class TopicController {

    private final KafkaTopicService topicService;

    public TopicController(KafkaTopicService topicService) {
        this.topicService = topicService;
    }

    // ---------- List topics ----------
    // ✅ Hybrid-friendly: works for BOTH session-auth and JWT-auth
    // Spring injects the Authentication built from either:
    // - HttpSession (stateful login)
    // - Bearer JWT (stateless)
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTopics(Authentication auth) {

        // ✅ Safe: no direct SecurityContextHolder usage
        log.info("AUTH principal={}, authorities={}", auth.getName(), auth.getAuthorities());

        List<String> topics = topicService.listTopics();

        Map<String, Object> payload = Map.of(
                "cluster", clusterName(),
                "count", topics.size(),
                "topics", topics
        );

        ApiResponse<Map<String, Object>> body = ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Topics fetched successfully")
                .data(payload)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.ok(body);
    }

    // ---------- Create topic (sync) ----------
    // ✅ Hybrid-friendly: you can also inject Authentication here if you want auditing
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TopicWithListResponse>> createTopic(
            @Valid @RequestBody CreateTopicRequest request,
            Authentication auth) {

        log.info("Request to create topic: {} (by user={})", request.getTopicName(), auth.getName());

        TopicActionResult action = topicService.createTopic(request);
        List<String> topics = topicService.listTopics();

        TopicWithListResponse topicResponse = TopicWithListResponse.builder()
                .action(action)
                .cluster(clusterName())
                .count(topics.size())
                .topics(topics)
                .build();

        boolean ok = action.isOk();

        ApiResponse<TopicWithListResponse> body = ApiResponse.<TopicWithListResponse>builder()
                .success(ok)
                .message(ok ? "Topic created successfully" : "Failed to create topic")
                .data(topicResponse)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity
                .status(ok ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

    // ---------- Delete topic (async at controller level) ----------
    // ⚠️ Note: CompletableFuture runs on a different thread (common pool).
    // If you need auth inside the async block, you must propagate SecurityContext.
    // Here we only log auth BEFORE the async boundary, so it's safe.
    @PostMapping("/delete")
    public CompletableFuture<ResponseEntity<ApiResponse<TopicWithListResponse>>> deleteTopic(
            @Valid @RequestBody DeleteTopicRequest request,
            Authentication auth) {

        log.info("Async request to delete topic: {} (by user={})", request.getTopicName(), auth.getName());

        return CompletableFuture.supplyAsync(() -> {

            TopicActionResult action = topicService.deleteTopic(request.getTopicName());
            List<String> topics = topicService.listTopics();

            TopicWithListResponse topicResponse = TopicWithListResponse.builder()
                    .action(action)
                    .cluster(clusterName())
                    .count(topics.size())
                    .topics(topics)
                    .build();

            boolean ok = action.isOk();

            ApiResponse<TopicWithListResponse> body = ApiResponse.<TopicWithListResponse>builder()
                    .success(ok)
                    .message(ok ? "Topic deleted successfully" : "Failed to delete topic")
                    .data(topicResponse)
                    .timestamp(ZonedDateTime.now())
                    .build();

            return ResponseEntity
                    .status(ok ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
        });
    }

    // ---------- Helpers ----------
    private String clusterName() {
        return "kraft-cluster-1";
    }
}
