package com.srikar.kafka.exception;

import java.util.UUID;

public class TopicNotFoundException extends RuntimeException {

    private final UUID clusterId;
    private final String topicName;

    public TopicNotFoundException(UUID clusterId, String topicName) {
        super("Topic not found: " + topicName + " (clusterId=" + clusterId + ")");
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
