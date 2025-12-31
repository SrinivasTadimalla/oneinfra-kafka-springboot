// src/main/java/com/srikar/kafka/controller/KafkaSchemaController.java
package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.schema.SchemaRegisterRequest;
import com.srikar.kafka.dto.schema.SchemaVersionDto;
import com.srikar.kafka.service.KafkaSchemaRegistryService;
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
        path = "/api/kafka/schemas",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
public class KafkaSchemaController {

    private final KafkaSchemaRegistryService schemaService;

    // -------------------------------------------------------
    // REGISTER (CREATE / UPDATE) SCHEMA
    // Topic Name Strategy:
    //   subject = <topic>-key | <topic>-value
    // -------------------------------------------------------
    @PostMapping(
            path = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<SchemaVersionDto>> register(
            @Valid @RequestBody SchemaRegisterRequest request
    ) {
        SchemaVersionDto data = schemaService.register(request);

        return ResponseEntity.ok(
                ApiResponses.ok("Schema registered successfully", data)
        );
    }

    // -------------------------------------------------------
    // GET LATEST SCHEMA VERSION
    // -------------------------------------------------------
    @GetMapping("/subjects/{subject}/latest")
    public ResponseEntity<ApiResponse<SchemaVersionDto>> getLatest(
            @RequestParam UUID clusterId,
            @PathVariable String subject
    ) {
        SchemaVersionDto data =
                schemaService.getLatest(clusterId, subject);

        return ResponseEntity.ok(
                ApiResponses.ok("Latest schema fetched successfully", data)
        );
    }

    // -------------------------------------------------------
    // GET ALL SCHEMA VERSIONS FOR A SUBJECT
    // -------------------------------------------------------
    @GetMapping("/subjects/{subject}/versions")
    public ResponseEntity<ApiResponse<List<SchemaVersionDto>>> getAllVersions(
            @RequestParam UUID clusterId,
            @PathVariable String subject
    ) {
        List<SchemaVersionDto> data =
                schemaService.getAllVersions(clusterId, subject);

        return ResponseEntity.ok(
                ApiResponses.ok("Schema versions fetched successfully", data)
        );
    }
}
