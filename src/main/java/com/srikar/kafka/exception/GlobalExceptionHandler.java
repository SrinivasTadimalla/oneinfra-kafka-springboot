package com.srikar.kafka.exception;

import com.srikar.kafka.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------
    // 1) Bean validation errors (@Valid)
    // ---------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .timestamp(ZonedDateTime.now())
                        .build());
    }

    // ---------------------------------------------------------
    // 2) Domain-level validation errors
    // ---------------------------------------------------------
    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiResponse<String>> handleDomainValidation(DomainValidationException ex) {

        log.warn("Domain validation error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(fail(ex.getMessage()));
    }

    // ---------------------------------------------------------
    // 3) Resource not found
    // ---------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(fail(ex.getMessage()));
    }

    // ---------------------------------------------------------
    // 4) Topic exceptions (NEW)
    // ---------------------------------------------------------
    @ExceptionHandler(DuplicateTopicException.class)
    public ResponseEntity<ApiResponse<String>> handleDuplicateTopic(DuplicateTopicException ex) {

        log.warn("Duplicate topic: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(fail(ex.getMessage()));
    }

    @ExceptionHandler(TopicNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleTopicNotFound(TopicNotFoundException ex) {

        log.warn("Topic not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(fail(ex.getMessage()));
    }

    // ---------------------------------------------------------
    // 5) Kafka operation failures
    // ---------------------------------------------------------
    @ExceptionHandler(KafkaOperationException.class)
    public ResponseEntity<ApiResponse<String>> handleKafkaOperation(KafkaOperationException ex) {

        log.error("Kafka operation failed: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(fail(ex.getMessage()));
    }

    // ---------------------------------------------------------
    // 6) Cluster operation failures
    // ---------------------------------------------------------
    @ExceptionHandler(ClusterOperationException.class)
    public ResponseEntity<ApiResponse<String>> handleClusterOperation(ClusterOperationException ex) {

        log.error("Cluster operation failed: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(fail(ex.getMessage()));
    }

    // ---------------------------------------------------------
    // 7) Catch-all unexpected exceptions
    // ---------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {

        log.error("Unexpected error occurred:", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(fail("Internal server error"));
    }

    // ---------------------------------------------------------
    // Small helper (keeps handlers consistent)
    // ---------------------------------------------------------
    private ApiResponse<String> fail(String message) {
        return ApiResponse.<String>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(ZonedDateTime.now())
                .build();
    }

}
