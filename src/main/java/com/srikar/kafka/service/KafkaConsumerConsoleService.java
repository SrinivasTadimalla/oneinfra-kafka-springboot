package com.srikar.kafka.service;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import com.srikar.kafka.config.KafkaAdminProperties;
import com.srikar.kafka.dto.consumer.ConsumerDto;
import com.srikar.kafka.dto.consumer.ConsumerRecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerConsoleService {

    private final KafkaBootstrapResolver bootstrapResolver;
    private final KafkaAdminProperties props;

    /**
     * One-shot fetch:
     * - creates consumer
     * - assigns partitions (all or subset)
     * - seeks based on position
     * - polls until maxMessages or poll timeout budget
     * - closes consumer
     */
    public ConsumerDto.FetchResponse fetch(ConsumerDto.FetchRequest req) {

        if (req == null) throw new IllegalArgumentException("Request body is missing");
        if (isBlank(req.clusterName())) throw new IllegalArgumentException("clusterName is required");
        if (isBlank(req.topicName())) throw new IllegalArgumentException("topicName is required");

        final String clusterName = req.clusterName().trim();
        final String topicName = req.topicName().trim();

        final String bootstrap = bootstrapResolver.resolve(clusterName);

        final int pollTimeoutMs = resolvePollTimeoutMs(req);
        final int maxMessages = resolveMaxMessages(req);

        // Budget for the overall fetch: don’t let this call hang forever.
        // You can tune this separately if you want.
        final int totalBudgetMs = Math.max(pollTimeoutMs, safeTimeoutMsInt());

        Properties p = new Properties();

        // ----------------------------
        // Core consumer config
        // ----------------------------
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.CLIENT_ID_CONFIG, "oneinfra-ui-consumer-" + UUID.randomUUID());

        // Console fetch should NOT join any real group (so we use a random group id)
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "oneinfra-ui-consumer-console-" + UUID.randomUUID());

        // We manually assign + seek; do not auto-commit.
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Read bytes (we’ll base64 encode to UI)
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        // Safety limits
        p.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, totalBudgetMs);
        p.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, totalBudgetMs);
        p.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, Math.max(30000, totalBudgetMs));
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Math.min(maxMessages, 500)); // cap per poll

        // ----------------------------
        // SSL / mTLS
        // ----------------------------
        KafkaAdminProperties.Ssl ssl = props.getSsl();
        if (ssl == null) {
            throw new IllegalStateException("Kafka SSL settings are missing (props.getSsl() == null)");
        }

        p.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, ssl.getSecurityProtocol()); // "SSL"

        p.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ssl.getTruststoreLocation());
        p.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.getTruststorePassword());
        p.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, ssl.getTruststoreType());

        p.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ssl.getKeystoreLocation());
        p.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ssl.getKeystorePassword());
        p.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, ssl.getKeystoreType());
        p.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, ssl.getKeyPassword());

        // IP brokers → disable hostname verification
        p.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                ssl.getEndpointIdentificationAlgorithm() == null ? "" : ssl.getEndpointIdentificationAlgorithm());

        List<ConsumerRecordDto> out = new ArrayList<>(Math.min(maxMessages, 200));

        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(p)) {

            // 1) Resolve partitions to read
            List<TopicPartition> tps = resolveTopicPartitions(consumer, topicName, req.partitions());
            if (tps.isEmpty()) {
                return new ConsumerDto.FetchResponse(clusterName, topicName, 0, List.of());
            }

            // 2) Assign (no group coordination)
            consumer.assign(tps);

            // 3) Seek based on requested start position
            applySeek(consumer, tps, req);

            // 4) Poll loop until maxMessages or budget exhausted
            long deadlineNs = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(totalBudgetMs);

            while (out.size() < maxMessages && System.nanoTime() < deadlineNs) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(pollTimeoutMs));
                if (records.isEmpty()) break;

                for (ConsumerRecord<byte[], byte[]> r : records) {
                    out.add(toDto(r));
                    if (out.size() >= maxMessages) break;
                }
            }

            return new ConsumerDto.FetchResponse(clusterName, topicName, out.size(), out);

        } catch (Exception e) {
            log.error("Fetch failed cluster={} topic={} bootstrap={}", clusterName, topicName, bootstrap, e);
            throw new RuntimeException("Fetch failed: " + safeMsg(e), e);
        }
    }

    // ----------------------------
    // Seek logic
    // ----------------------------

    private void applySeek(KafkaConsumer<byte[], byte[]> consumer,
                           List<TopicPartition> tps,
                           ConsumerDto.FetchRequest req) {

        ConsumerDto.Position pos = req.position() == null ? ConsumerDto.Position.LATEST : req.position();

        switch (pos) {
            case EARLIEST -> consumer.seekToBeginning(tps);
            case LATEST -> consumer.seekToEnd(tps);

            case OFFSET -> {
                if (req.offset() == null || req.offset() < 0) {
                    throw new IllegalArgumentException("offset is required and must be >= 0 when position=OFFSET");
                }
                long off = req.offset();
                for (TopicPartition tp : tps) {
                    consumer.seek(tp, off);
                }
            }

            case TIMESTAMP -> {
                if (req.timestampMs() == null || req.timestampMs() <= 0) {
                    throw new IllegalArgumentException("timestampMs is required and must be > 0 when position=TIMESTAMP");
                }
                Map<TopicPartition, Long> query = new HashMap<>();
                for (TopicPartition tp : tps) query.put(tp, req.timestampMs());

                Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> offsets =
                        consumer.offsetsForTimes(query);

                // If broker returns null (no offset at that timestamp), fall back to end
                for (TopicPartition tp : tps) {
                    var oat = offsets.get(tp);
                    if (oat != null) consumer.seek(tp, oat.offset());
                    else consumer.seekToEnd(List.of(tp));
                }
            }
        }
    }

    private List<TopicPartition> resolveTopicPartitions(KafkaConsumer<byte[], byte[]> consumer,
                                                        String topic,
                                                        List<Integer> requested) {

        var partitions = consumer.partitionsFor(topic);
        if (partitions == null || partitions.isEmpty()) return List.of();

        Set<Integer> all = partitions.stream().map(pi -> pi.partition()).collect(Collectors.toSet());

        // If user didn’t pass partitions → all
        if (requested == null || requested.isEmpty()) {
            return all.stream().sorted().map(p -> new TopicPartition(topic, p)).toList();
        }

        // Validate subset
        for (Integer p : requested) {
            if (p == null || !all.contains(p)) {
                throw new IllegalArgumentException("Invalid partition: " + p + " for topic: " + topic);
            }
        }

        return requested.stream()
                .distinct()
                .sorted()
                .map(p -> new TopicPartition(topic, p))
                .toList();
    }

    // ----------------------------
    // Mapping helpers
    // ----------------------------

    private ConsumerRecordDto toDto(ConsumerRecord<byte[], byte[]> r) {
        String keyB64 = r.key() == null ? null : Base64.getEncoder().encodeToString(r.key());
        String valB64 = r.value() == null ? null : Base64.getEncoder().encodeToString(r.value());

        // Optional convenience: show headers as "k=v; k2=v2" where v is UTF-8 best-effort
        String headers = null;
        if (r.headers() != null) {
            List<String> parts = new ArrayList<>();
            for (Header h : r.headers()) {
                String v = h.value() == null ? "" : safeUtf8(h.value());
                parts.add(h.key() + "=" + v);
            }
            headers = parts.isEmpty() ? null : String.join("; ", parts);
        }

        Integer sizeBytes = null;
        try {
            // r.serializedValueSize() exists in Kafka ConsumerRecord
            int vSize = r.serializedValueSize();
            int kSize = r.serializedKeySize();
            if (vSize >= 0 || kSize >= 0) sizeBytes = Math.max(0, vSize) + Math.max(0, kSize);
        } catch (Exception ignored) {
        }

        return new ConsumerRecordDto(
                r.partition(),
                r.offset(),
                r.timestamp(),
                keyB64,
                valB64,
                headers,
                sizeBytes
        );
    }

    private String safeUtf8(byte[] bytes) {
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    // ----------------------------
    // Defaults / guards
    // ----------------------------

    private int resolvePollTimeoutMs(ConsumerDto.FetchRequest req) {
        Integer ms = req.pollTimeoutMs();
        if (ms == null) return 1000;
        if (ms < 100) return 100;
        if (ms > 10000) return 10000;
        return ms;
    }

    private int resolveMaxMessages(ConsumerDto.FetchRequest req) {
        Integer m = req.maxMessages();
        if (m == null) return 50;
        if (m < 1) return 1;
        if (m > 500) return 500; // UI safety cap
        return m;
    }

    private int safeTimeoutMsInt() {
        Integer ms = props.getDefaultApiTimeoutMs();
        int resolved = (ms == null || ms < 1000) ? 15000 : ms;
        if (resolved < 1000) resolved = 15000;
        return resolved;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        return m == null ? "" : (m.length() > 500 ? m.substring(0, 500) : m);
    }
}
