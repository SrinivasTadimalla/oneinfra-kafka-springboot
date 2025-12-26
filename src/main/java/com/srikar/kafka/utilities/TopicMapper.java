package com.srikar.kafka.utilities;

import com.srikar.kafka.dto.topic.TopicDetail;
import com.srikar.kafka.dto.topic.TopicSummary;
import com.srikar.kafka.entity.KafkaTopicEntity;

import java.util.Collections;

public final class TopicMapper {

    private TopicMapper() {}

    public static TopicSummary toSummary(KafkaTopicEntity e) {
        if (e == null) return null;

        return TopicSummary.builder()
                .id(e.getId())
                .clusterId(e.getClusterId())
                .topicName(e.getTopicName())
                .partitions(e.getPartitions())
                .replicationFactor(e.getReplicationFactor())
                .enabled(e.isEnabled())
                .status(e.getStatus())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static TopicDetail toDetail(KafkaTopicEntity e) {
        if (e == null) return null;

        return TopicDetail.builder()
                .id(e.getId())
                .clusterId(e.getClusterId())
                .topicName(e.getTopicName())
                .description(e.getDescription())
                .partitions(e.getPartitions())
                .replicationFactor(e.getReplicationFactor())
                .enabled(e.isEnabled())
                .status(e.getStatus())
                .lastError(null)
                .configs(Collections.emptyMap())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
