package com.srikar.kafka.utilities;

import com.srikar.kafka.api.ApiResponse;

import java.time.ZonedDateTime;

/**
 * Utility class to build standardized API response envelopes
 * across all Kafka REST endpoints.
 *
 * Why this exists:
 * - Ensures a consistent response structure for UI (Angular MFE)
 * - Avoids duplicating ApiResponse.builder() logic in controllers
 * - Keeps success / failure semantics uniform across services
 *
 * NOTE:
 * - This class contains ONLY static helpers
 * - It should NOT contain business logic
 */
public final class ApiResponses {

    /**
     * Private constructor to prevent instantiation.
     * This is a pure utility/helper class.
     */
    private ApiResponses() {
    }

    /**
     * Build a successful API response.
     *
     * Use this when:
     * - A request completes successfully
     * - Data is available and should be returned to the client
     *
     * Typical usage:
     *
     * return ResponseEntity.ok(
     *     ApiResponses.ok("Kafka cluster saved successfully", data)
     * );
     *
     * @param message Human-readable success message
     * @param data    Payload to return to the client
     * @param <T>     Type of response payload
     * @return standardized ApiResponse with success=true
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Build a failure API response.
     *
     * Use this when:
     * - A request completes but business logic fails
     * - The operation is valid but the outcome is unsuccessful
     *
     * IMPORTANT:
     * - This does NOT replace exception handling
     * - Exceptions should be handled via GlobalExceptionHandler
     *
     * Typical usage:
     *
     * return ResponseEntity.badRequest()
     *     .body(ApiResponses.fail("Kafka cluster not found", null));
     *
     * @param message Human-readable failure message
     * @param data    Optional payload (can be null)
     * @param <T>     Type of response payload
     * @return standardized ApiResponse with success=false
     */
    public static <T> ApiResponse<T> fail(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }
}
