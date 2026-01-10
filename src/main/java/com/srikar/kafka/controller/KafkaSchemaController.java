package com.srikar.kafka.controller;

import com.srikar.kafka.api.ApiResponse;
import com.srikar.kafka.dto.schema.SchemaRegisterRequest;
import com.srikar.kafka.dto.schema.SchemaSubjectSummaryDto;
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
    // POST /api/kafka/schemas/register
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
    // ✅ LIST SUBJECTS (for dropdown)
    // GET /api/kafka/schemas/subjects?clusterId=...
    // -------------------------------------------------------
    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SchemaSubjectSummaryDto>>> listSubjects(
            @RequestParam UUID clusterId
    ) {
        List<SchemaSubjectSummaryDto> data = schemaService.listSubjects(clusterId);

        return ResponseEntity.ok(
                ApiResponses.ok("Subjects fetched successfully", data)
        );
    }

    // -------------------------------------------------------
    // GET LATEST SCHEMA VERSION
    // GET /api/kafka/schemas/subjects/{subject}/latest?clusterId=...
    // -------------------------------------------------------
    @GetMapping("/subjects/{subject}/latest")
    public ResponseEntity<ApiResponse<SchemaVersionDto>> getLatest(
            @RequestParam UUID clusterId,
            @PathVariable String subject
    ) {
        SchemaVersionDto data = schemaService.getLatest(clusterId, subject);

        return ResponseEntity.ok(
                ApiResponses.ok("Latest schema fetched successfully", data)
        );
    }

    // -------------------------------------------------------
    // GET ALL SCHEMA VERSIONS FOR A SUBJECT
    // GET /api/kafka/schemas/subjects/{subject}/versions?clusterId=...
    // -------------------------------------------------------
    @GetMapping("/subjects/{subject}/versions")
    public ResponseEntity<ApiResponse<List<SchemaVersionDto>>> getAllVersions(
            @RequestParam UUID clusterId,
            @PathVariable String subject
    ) {
        List<SchemaVersionDto> data = schemaService.getAllVersions(clusterId, subject);

        return ResponseEntity.ok(
                ApiResponses.ok("Schema versions fetched successfully", data)
        );
    }

    // -------------------------------------------------------
    // ✅ GET A SPECIFIC VERSION
    // GET /api/kafka/schemas/subjects/{subject}/versions/{version}?clusterId=...
    // -------------------------------------------------------
    @GetMapping("/subjects/{subject}/versions/{version}")
    public ResponseEntity<ApiResponse<SchemaVersionDto>> getByVersion(
            @RequestParam UUID clusterId,
            @PathVariable String subject,
            @PathVariable int version
    ) {
        SchemaVersionDto data = schemaService.getByVersion(clusterId, subject, version);

        return ResponseEntity.ok(
                ApiResponses.ok("Schema version fetched successfully", data)
        );
    }

    // -------------------------------------------------------
    // ✅ DELETE SUBJECT (HARD DELETE)
    // DELETE /api/kafka/schemas/subjects/{subject}?clusterId=...
    // -------------------------------------------------------
    @DeleteMapping("/subjects/{subject}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(
            @RequestParam UUID clusterId,
            @PathVariable String subject
    ) {
        schemaService.deleteSubject(clusterId, subject);

        return ResponseEntity.ok(
                ApiResponses.ok("Schema subject deleted successfully", null)
        );
    }
}
