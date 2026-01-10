package com.srikar.kafka.service;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import com.srikar.kafka.config.KafkaAdminProperties;
import com.srikar.kafka.dto.producer.ProducerDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerConsoleService {

    private final KafkaBootstrapResolver bootstrapResolver;
    private final KafkaAdminProperties props;

    /**
     * ✅ Schema validation (STUB for now)
     * Later: call your schema registry service + validate payload.
     */
    public ProducerDto.ValidateResponse validate(ProducerDto.ValidateRequest req) {

        // Basic guard rails
        if (req == null) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("", "Request body is missing")));
        }

        if (isBlank(req.getClusterName())) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("clusterName", "clusterName is required")));
        }

        if (isBlank(req.getTopicName())) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("topicName", "topicName is required")));
        }

        if (isBlank(req.getPayload())) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("payload", "payload is required")));
        }

        if (req.getSchemaRef() == null) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("schemaRef", "schemaRef is required for schema validation")));
        }

        if (req.getSchemaRef().getSchemaType() == null) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("schemaRef.schemaType", "schemaType is required")));
        }

        if (isBlank(req.getSchemaRef().getSubject())) {
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("schemaRef.subject", "subject is required")));
        }

        // ✅ STUB success: resolve version
        Integer resolved = (req.getSchemaRef().getVersion() == null) ? 1 : req.getSchemaRef().getVersion();
        return new ProducerDto.ValidateResponse(true, resolved, null);

    }

    /**
     * ✅ Publish RAW (and later SCHEMA after validation wiring)
     */
    public ProducerDto.PublishResponse publish(ProducerDto.PublishRequest req) {

        if (req == null) {
            throw new IllegalArgumentException("Request body is missing");
        }

        if (isBlank(req.getClusterName())) {
            throw new IllegalArgumentException("clusterName is required");
        }

        if (isBlank(req.getTopicName())) {
            throw new IllegalArgumentException("topicName is required");
        }

        if (isBlank(req.getPayload())) {
            throw new IllegalArgumentException("payload is required");
        }

        String clusterName = req.getClusterName().trim();
        String bootstrap = bootstrapResolver.resolve(clusterName);

        long timeoutMs = safeTimeoutMs();

        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        // keep console isolated & predictable
        p.put(ProducerConfig.CLIENT_ID_CONFIG, "oneinfra-producer-console-" + UUID.randomUUID());

        // serializers: bytes (we send UTF-8 bytes)
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        // safe defaults
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, String.valueOf(timeoutMs));
        p.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(timeoutMs));
        p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        byte[] keyBytes = isBlank(req.getKey()) ? null : req.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = req.getPayload().getBytes(StandardCharsets.UTF_8);

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(req.getTopicName().trim(), keyBytes, valueBytes);

        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(p)) {

            RecordMetadata md = producer.send(record).get(timeoutMs, TimeUnit.MILLISECONDS);

            return new ProducerDto.PublishResponse(
                    md.topic(),
                    md.partition(),
                    md.offset(),
                    md.timestamp() > 0 ? md.timestamp() : Instant.now().toEpochMilli()
            );

        } catch (Exception e) {
            log.error("Publish failed. cluster={} topic={} bootstrap={}",
                    req.getClusterName(), req.getTopicName(), bootstrap, e);
            throw new RuntimeException("Publish failed: " + safeMsg(e), e);
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private long safeTimeoutMs() {
        Integer ms = props.getDefaultApiTimeoutMs();
        return (ms == null || ms < 1000) ? 15000L : ms.longValue();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) return "";
        return m.length() > 500 ? m.substring(0, 500) : m;
    }
}
