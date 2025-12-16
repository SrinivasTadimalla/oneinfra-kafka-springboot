package com.srikar.kafka.exception;

public class ClusterOperationException extends RuntimeException {

    public ClusterOperationException(String message) {
        super(message);
    }

    public ClusterOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
