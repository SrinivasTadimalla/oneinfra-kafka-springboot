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
    // 1. Bean validation errors (@Valid)
    // ---------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ---------------------------------------------------------
    // 2. Domain-level validation errors
    // ---------------------------------------------------------
    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ApiResponse<String>> handleDomainValidation(DomainValidationException ex) {

        log.warn("Domain validation error: {}", ex.getMessage());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ---------------------------------------------------------
    // 3. Resource not found
    // ---------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ---------------------------------------------------------
    // 4. Kafka operation failures
    // ---------------------------------------------------------
    @ExceptionHandler(KafkaOperationException.class)
    public ResponseEntity<ApiResponse<String>> handleKafkaOperation(KafkaOperationException ex) {

        log.error("Kafka operation failed: {}", ex.getMessage(), ex);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(ZonedDateTime.now())
                .build();

        // Kafka failures are usually 500 unless you want 502/503
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ---------------------------------------------------------
    // 5. Cluster operation failures
    // ---------------------------------------------------------
    @ExceptionHandler(ClusterOperationException.class)
    public ResponseEntity<ApiResponse<String>> handleClusterOperation(ClusterOperationException ex) {

        log.error("Cluster operation failed: {}", ex.getMessage(), ex);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ---------------------------------------------------------
    // 6. Catch-all unexpected exceptions
    // ---------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {

        log.error("Unexpected error occurred:", ex);

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message("Internal server error")
                .data(null)
                .timestamp(ZonedDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
