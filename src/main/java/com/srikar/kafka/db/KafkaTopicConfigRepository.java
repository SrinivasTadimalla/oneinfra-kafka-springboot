package com.srikar.kafka.db;

import com.srikar.kafka.entity.KafkaTopicConfigEntity;
import com.srikar.kafka.entity.KafkaTopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KafkaTopicConfigRepository extends JpaRepository<KafkaTopicConfigEntity, UUID> {

    List<KafkaTopicConfigEntity> findByTopic(KafkaTopicEntity topic);

    Optional<KafkaTopicConfigEntity> findByTopicAndConfigKey(KafkaTopicEntity topic, String configKey);

    void deleteByTopic(KafkaTopicEntity topic);
}
