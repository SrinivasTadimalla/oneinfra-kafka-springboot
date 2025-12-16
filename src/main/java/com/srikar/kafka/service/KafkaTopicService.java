package com.srikar.kafka.service;

import com.srikar.kafka.domain.TopicActionResult;
import com.srikar.kafka.dto.CreateTopicRequest;
import com.srikar.kafka.exception.DomainValidationException;
import com.srikar.kafka.exception.KafkaOperationException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class KafkaTopicService {

    private final AdminClient adminClient;

    public KafkaTopicService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    // ----------------------------------------------------
    // LIST TOPICS  (sync)
    // ----------------------------------------------------
    public List<String> listTopics() {
        try {
            List<String> result = new ArrayList<>(adminClient.listTopics().names().get());
            Collections.sort(result);
            return result;
        } catch (Exception e) {
            log.error("Failed to list topics", e);
            return Collections.emptyList();
        }
    }

    // ----------------------------------------------------
    // CREATE TOPIC  (sync, Circuit Breaker + Retry)
    // ----------------------------------------------------
    @CircuitBreaker(name = "kafkaAdmin", fallbackMethod = "createTopicFallback")
    @Retry(name = "kafkaAdmin")
    public TopicActionResult createTopic(CreateTopicRequest req) {

        String topic = req.getTopicName();

        if (topic == null || topic.isBlank()) {
            throw new DomainValidationException("Topic name must not be blank");
        }

        NewTopic newTopic = new NewTopic(
                topic,
                req.getPartitions(),
                req.getReplicationFactor()
        );

        try {
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();

            return new TopicActionResult(
                    true,
                    topic,
                    "create",
                    "Topic created successfully"
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaOperationException("Interrupted while creating topic: " + topic, e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof TopicExistsException) {
                throw new DomainValidationException("Topic already exists: " + topic);
            }

            throw new KafkaOperationException("Failed to create topic: " + topic, cause);
        }
    }

    // Fallback for createTopic()
    @SuppressWarnings("unused")
    private TopicActionResult createTopicFallback(CreateTopicRequest req, Throwable t) {
        String topic = req.getTopicName();
        log.error("createTopic FALLBACK triggered for topic: {}", topic, t);

        String message = "Kafka unavailable or circuit open while creating topic: "
                + (t.getMessage() != null ? t.getMessage() : "no details");

        return new TopicActionResult(
                false,
                topic,
                "create",
                message
        );
    }

    // ----------------------------------------------------
    // DELETE TOPIC  (sync, Circuit Breaker + Retry)
    // ----------------------------------------------------
    @CircuitBreaker(name = "kafkaAdmin", fallbackMethod = "deleteTopicFallback")
    @Retry(name = "kafkaAdmin")
    public TopicActionResult deleteTopic(String topicName) {

        try {
            Set<String> topics = Collections.singleton(topicName);
            DeleteTopicsResult result = adminClient.deleteTopics(topics);
            KafkaFuture<Void> future = result.all();

            future.get();  // blocking wait

            return new TopicActionResult(
                    true,
                    topicName,
                    "delete",
                    "Topic deleted successfully"
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaOperationException("Interrupted while deleting topic: " + topicName, e);

        } catch (ExecutionException e) {
            throw new KafkaOperationException("Failed to delete topic: " + topicName, e.getCause());
        }
    }

    // Fallback for deleteTopic()
    @SuppressWarnings("unused")
    private TopicActionResult deleteTopicFallback(String topicName, Throwable t) {
        log.error("deleteTopic FALLBACK triggered for topic: {}", topicName, t);

        String message = "Kafka unavailable or circuit open while deleting topic: "
                + (t.getMessage() != null ? t.getMessage() : "no details");

        return new TopicActionResult(
                false,
                topicName,
                "delete",
                message
        );
    }
}
