package com.srikar.kafka.utilities;

import com.srikar.kafka.dto.topic.TopicDetail;
import com.srikar.kafka.dto.topic.TopicSummary;
import com.srikar.kafka.entity.KafkaTopicEntity;

import java.util.Collections;
import java.util.UUID;

public final class TopicMapper {

    private TopicMapper() {}

    public static TopicSummary toSummary(KafkaTopicEntity e) {
        if (e == null) return null;

        return TopicSummary.builder()
                .id(e.getId())
                .clusterId(resolveClusterId(e))
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
                .clusterId(resolveClusterId(e))
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

    private static UUID resolveClusterId(KafkaTopicEntity e) {
        // ✅ Prefer relationship (always correct right after save)
        if (e.getCluster() != null && e.getCluster().getId() != null) {
            return e.getCluster().getId();
        }
        // fallback (works when entity is loaded from DB)
        return e.getClusterId();
    }
}
