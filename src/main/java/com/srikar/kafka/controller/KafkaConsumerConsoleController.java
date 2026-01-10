package com.srikar.kafka.controller;

import com.srikar.kafka.dto.consumer.ConsumerDto;
import com.srikar.kafka.service.KafkaConsumerConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kafka/consumer")
public class KafkaConsumerConsoleController {

    private final KafkaConsumerConsoleService consumerService;

    /**
     * FETCH mode (one-shot read).
     * UI calls this when user clicks "FETCH".
     *
     * Body: ConsumerDto.FetchRequest
     * Resp: ConsumerDto.FetchResponse
     */
    @PostMapping("/fetch")
    public ResponseEntity<ConsumerDto.FetchResponse> fetch(@RequestBody ConsumerDto.FetchRequest req) {
        return ResponseEntity.ok(consumerService.fetch(req));
    }

    /**
     * (Optional placeholder)
     * If later you implement true "TAIL" server-side (SSE/WebSocket),
     * you can add endpoint(s) here.
     */
}
