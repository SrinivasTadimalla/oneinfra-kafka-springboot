package com.srikar.kafka.exception;

import java.util.UUID;

public class DuplicateTopicException extends RuntimeException {

    private final UUID clusterId;
    private final String topicName;

    public DuplicateTopicException(UUID clusterId, String topicName) {
        super("Topic already exists: " + topicName + " (clusterId=" + clusterId + ")");
        this.clusterId = clusterId;
        this.topicName = topicName;
    }

    public UUID getClusterId() {
        return clusterId;
    }

    public String getTopicName() {
        return topicName;
    }
}
