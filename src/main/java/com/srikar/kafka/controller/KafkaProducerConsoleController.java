package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.producer.ProducerDto;
import com.srikar.kafka.service.KafkaProducerConsoleService;
import com.srikar.kafka.utilities.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(
        path = "/api/kafka/producer",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class KafkaProducerConsoleController {

    private final KafkaProducerConsoleService producerService;

    /**
     * UI: Validate button (schema-mode)
     * POST /api/kafka/producer/validate
     */
    @PostMapping(
            path = "/validate",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ProducerDto.ValidateResponse>> validate(
            @RequestBody ProducerDto.ValidateRequest req
    ) {
        ProducerDto.ValidateResponse result = producerService.validate(req);

        return ResponseEntity.ok(
                ApiResponses.ok("Validation completed", result)
        );
    }

    /**
     * UI: Publish button (raw or schema-mode)
     * POST /api/kafka/producer/publish
     */
    @PostMapping(
            path = "/publish",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<ProducerDto.PublishResponse>> publish(
            @RequestBody ProducerDto.PublishRequest req
    ) {
        ProducerDto.PublishResponse result = producerService.publish(req);

        return ResponseEntity.ok(
                ApiResponses.ok("Message published successfully", result)
        );
    }
}
