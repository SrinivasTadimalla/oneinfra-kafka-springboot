package com.srikar.kafka.utilities;

import com.srikar.kafka.dto.schema.SchemaPart;

public final class SchemaSubjects {

    private SchemaSubjects() {}

    // TopicNameStrategy (production-grade)
    // <topic>-key or <topic>-value
    public static String topicSubject(String topicName, SchemaPart part) {
        if (topicName == null || topicName.trim().isEmpty()) {
            throw new IllegalArgumentException("topicName is required");
        }
        if (part == null) {
            throw new IllegalArgumentException("part is required");
        }
        return topicName.trim() + "-" + part.name().toLowerCase(); // key/value
    }
}
