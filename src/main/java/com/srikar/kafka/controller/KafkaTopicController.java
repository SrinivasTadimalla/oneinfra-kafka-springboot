package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.topic.TopicCreateRequest;
import com.srikar.kafka.dto.topic.TopicDetail;
import com.srikar.kafka.dto.topic.TopicSummary;
import com.srikar.kafka.dto.topic.TopicUpdateRequest;
import com.srikar.kafka.service.KafkaTopicService;
import com.srikar.kafka.utilities.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(
        path = "/api/kafka/topics",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class KafkaTopicController {

    private final KafkaTopicService topicService;

    // -------------------------------------------------------
    // CREATE
    // POST /api/kafka/topics
    // -------------------------------------------------------
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TopicDetail>> create(
            @Valid @RequestBody TopicCreateRequest req
    ) {
        TopicDetail created = topicService.createTopic(req);

        return ResponseEntity.ok(
                ApiResponses.ok("Kafka topic created successfully", created)
        );
    }

    // -------------------------------------------------------
    // LIST
    // GET /api/kafka/topics?clusterId=<uuid>
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<ApiResponse<List<TopicSummary>>> list(
            @RequestParam UUID clusterId
    ) {
        List<TopicSummary> list = topicService.listTopics(clusterId);

        return ResponseEntity.ok(
                ApiResponses.ok("Kafka topics fetched successfully", list)
        );
    }

    // -------------------------------------------------------
    // GET ONE (DB-backed only)
    // GET /api/kafka/topics/{topicName}?clusterId=<uuid>
    // -------------------------------------------------------
    @GetMapping("/{topicName}")
    public ResponseEntity<ApiResponse<TopicDetail>> getOne(
            @RequestParam UUID clusterId,
            @PathVariable String topicName
    ) {
        TopicDetail detail = topicService.getTopic(clusterId, topicName);

        return ResponseEntity.ok(
                ApiResponses.ok("Kafka topic fetched successfully", detail)
        );
    }

    // -------------------------------------------------------
    // GET ONE + KAFKA CONFIGS (NEW)
    // GET /api/kafka/topics/{topicName}/configs?clusterId=<uuid>
    // -------------------------------------------------------
    @GetMapping("/{topicName}/configs")
    public ResponseEntity<ApiResponse<TopicDetail>> getWithConfigs(
            @RequestParam UUID clusterId,
            @PathVariable String topicName
    ) {
        TopicDetail detail = topicService.getTopicWithConfigs(clusterId, topicName);

        return ResponseEntity.ok(
                ApiResponses.ok("Kafka topic configs fetched successfully", detail)
        );
    }

    // -------------------------------------------------------
    // UPDATE
    // PUT /api/kafka/topics/{topicName}?clusterId=<uuid>
    // -------------------------------------------------------
    @PutMapping(path = "/{topicName}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TopicDetail>> update(
            @RequestParam UUID clusterId,
            @PathVariable String topicName,
            @Valid @RequestBody TopicUpdateRequest req
    ) {
        TopicDetail updated = topicService.updateTopic(clusterId, topicName, req);

        return ResponseEntity.ok(
                ApiResponses.ok("Topic updated", updated)
        );
    }

    // -------------------------------------------------------
    // DELETE
    // DELETE /api/kafka/topics/{topicName}?clusterId=<uuid>
    // -------------------------------------------------------
    @DeleteMapping("/{topicName}")
    public ResponseEntity<ApiResponse<Object>> delete(
            @RequestParam UUID clusterId,
            @PathVariable String topicName
    ) {
        topicService.deleteTopic(clusterId, topicName);

        return ResponseEntity.ok(
                ApiResponses.ok("Topic deleted", null)
        );
    }

    // -------------------------------------------------------
    // GET ONE (DB or DB+Kafka configs via flag)
    // GET /api/kafka/topics/{topicName}/detail?clusterId=<uuid>&withConfigs=true
    // -------------------------------------------------------
    @GetMapping("/{topicName}/detail")
    public ResponseEntity<ApiResponse<TopicDetail>> getTopicDetail(
            @RequestParam UUID clusterId,
            @PathVariable String topicName,
            @RequestParam(defaultValue = "false") boolean withConfigs
    ) {
        TopicDetail data = withConfigs
                ? topicService.getTopicWithConfigs(clusterId, topicName)
                : topicService.getTopic(clusterId, topicName);

        return ResponseEntity.ok(
                ApiResponses.ok("Kafka topic fetched successfully", data)
        );
    }


}
