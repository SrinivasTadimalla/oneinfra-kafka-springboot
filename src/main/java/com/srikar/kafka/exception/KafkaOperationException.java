package com.srikar.kafka.exception;

public class KafkaOperationException extends RuntimeException {

    public KafkaOperationException(String message) {
        super(message);
    }

    public KafkaOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
