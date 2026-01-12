package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.consumer.ConsumerDto;
import com.srikar.kafka.dto.consumer.ConsumerTailRequest;
import com.srikar.kafka.dto.consumer.ConsumerTailResponse;
import com.srikar.kafka.service.KafkaConsumerConsoleService;
import com.srikar.kafka.utilities.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        path = "/api/kafka/consumer",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class KafkaConsumerConsoleController {

    private final KafkaConsumerConsoleService consumerService;

    @PostMapping(
            path = "/fetch",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ConsumerDto.FetchResponse>> fetch(
            @RequestBody ConsumerDto.FetchRequest req
    ) {
        ConsumerDto.FetchResponse result = consumerService.fetch(req);

        return ResponseEntity.ok(
                ApiResponses.ok("Messages fetched successfully", result)
        );
    }

    @PostMapping(
            path = "/tail",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ConsumerTailResponse>> tail(
            @Valid @RequestBody ConsumerTailRequest req
    ) {
        ConsumerTailResponse result = consumerService.tail(req);

        return ResponseEntity.ok(
                ApiResponses.ok("Tail poll executed successfully", result)
        );
    }
}
