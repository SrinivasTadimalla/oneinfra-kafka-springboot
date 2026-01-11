package com.srikar.kafka.service;

import com.srikar.kafka.config.KafkaAdminProperties;
import com.srikar.kafka.config.KafkaClientPropertiesFactory;
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

    private final KafkaClientPropertiesFactory clientProps; // ✅ shared base (bootstrap + SSL)
    private final KafkaAdminProperties props;               // ✅ only for timeouts, defaults

    // ----------------------------
    // Validate (schema stub)
    // ----------------------------
    public ProducerDto.ValidateResponse validate(ProducerDto.ValidateRequest req) {

        if (req == null)
            return new ProducerDto.ValidateResponse(false, null,
                    List.of(new ProducerDto.ValidationError("", "Request body is missing")));

        if (isBlank(req.getClusterName()))
            return error("clusterName", "clusterName is required");

        if (isBlank(req.getTopicName()))
            return error("topicName", "topicName is required");

        if (isBlank(req.getPayload()))
            return error("payload", "payload is required");

        if (req.getSchemaRef() == null)
            return error("schemaRef", "schemaRef is required for schema validation");

        if (req.getSchemaRef().getSchemaType() == null)
            return error("schemaRef.schemaType", "schemaType is required");

        if (isBlank(req.getSchemaRef().getSubject()))
            return error("schemaRef.subject", "subject is required");

        Integer resolved = req.getSchemaRef().getVersion() == null ? 1 : req.getSchemaRef().getVersion();
        return new ProducerDto.ValidateResponse(true, resolved, null);
    }

    // ----------------------------
    // Publish
    // ----------------------------
    public ProducerDto.PublishResponse publish(ProducerDto.PublishRequest req) {

        if (req == null) throw new IllegalArgumentException("Request body is missing");
        if (isBlank(req.getClusterName())) throw new IllegalArgumentException("clusterName is required");
        if (isBlank(req.getTopicName())) throw new IllegalArgumentException("topicName is required");
        if (isBlank(req.getPayload())) throw new IllegalArgumentException("payload is required");

        String clusterName = req.getClusterName().trim();
        int timeoutMs = safeTimeoutMsInt();

        // ✅ base includes bootstrap + SSL/mTLS + endpoint identification algo
        Properties p = clientProps.base(clusterName);

        // ----------------------------
        // Producer-specific core
        // ----------------------------
        p.put(ProducerConfig.CLIENT_ID_CONFIG, "oneinfra-ui-producer-" + UUID.randomUUID());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        // ----------------------------
        // Reliability
        // ----------------------------
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        p.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // ----------------------------
        // Prevent hanging forever (Kafka expects Integer)
        // ----------------------------
        p.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, timeoutMs);
        p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, timeoutMs);
        p.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, timeoutMs);

        byte[] key = isBlank(req.getKey()) ? null : req.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] value = req.getPayload().getBytes(StandardCharsets.UTF_8);

        ProducerRecord<byte[], byte[]> record =
                new ProducerRecord<>(req.getTopicName().trim(), key, value);

        try (KafkaProducer<byte[], byte[]> producer = new KafkaProducer<>(p)) {

            RecordMetadata md = producer.send(record).get((long) timeoutMs, TimeUnit.MILLISECONDS);

            return new ProducerDto.PublishResponse(
                    md.topic(),
                    md.partition(),
                    md.offset(),
                    md.timestamp() > 0 ? md.timestamp() : Instant.now().toEpochMilli()
            );

        } catch (Exception e) {
            log.error("Publish failed cluster={} topic={}", clusterName, req.getTopicName(), e);
            throw new RuntimeException("Publish failed: " + safeMsg(e), e);
        }
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private ProducerDto.ValidateResponse error(String field, String msg) {
        return new ProducerDto.ValidateResponse(false, null,
                List.of(new ProducerDto.ValidationError(field, msg)));
    }

    private int safeTimeoutMsInt() {
        Integer ms = props.getDefaultApiTimeoutMs();
        int resolved = (ms == null || ms < 1000) ? 15000 : ms;
        return Math.max(resolved, 1000);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        return m == null ? "" : (m.length() > 500 ? m.substring(0, 500) : m);
    }
}
