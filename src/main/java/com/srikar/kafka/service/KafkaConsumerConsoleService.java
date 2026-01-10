package com.srikar.kafka.service;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import com.srikar.kafka.config.KafkaAdminProperties;
import com.srikar.kafka.dto.consumer.ConsumerDto;
import com.srikar.kafka.dto.consumer.ConsumerRecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.Base64.Encoder;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerConsoleService {

    private final KafkaBootstrapResolver bootstrapResolver;
    private final KafkaAdminProperties props;

    private static final Encoder B64 = Base64.getEncoder();

    public ConsumerDto.FetchResponse fetch(ConsumerDto.FetchRequest req) {

        if (req == null) {
            return new ConsumerDto.FetchResponse(null, null, 0, List.of());
        }

        String clusterName = safe(req.clusterName());
        String topic = safe(req.topic());

        if (clusterName.isBlank() || topic.isBlank()) {
            return new ConsumerDto.FetchResponse(clusterName, topic, 0, List.of());
        }

        // 1) Resolve bootstrap (Redis-first like your other services)
        String bootstrap = bootstrapResolver.resolve(clusterName);

        // 2) Build consumer props (ephemeral console consumer)
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);

        // Console consumer pattern: assign + seek, no commits
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Keep it isolated
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "oneinfra-console-" + UUID.randomUUID());

        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        // Optional tuning
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(safeMax(req.maxMessages())));
        p.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(safeTimeoutMs()));
        p.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(safeTimeoutMs()));

        List<ConsumerRecordDto> out = new ArrayList<>();

        try (KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(p)) {

            // 3) Decide partitions
            List<TopicPartition> tps = resolveTopicPartitions(consumer, topic, req.partitions());

            if (tps.isEmpty()) {
                return new ConsumerDto.FetchResponse(clusterName, topic, 0, List.of());
            }

            // 4) Assign (NO group coordination)
            consumer.assign(tps);

            // 5) Seek based on start position
            applyStartPosition(consumer, tps, req);

            // 6) Poll and collect
            Duration poll = Duration.ofMillis(safePollTimeoutMs(req.pollTimeoutMs()));
            int target = safeMax(req.maxMessages());

            while (out.size() < target) {
                var records = consumer.poll(poll);
                if (records.isEmpty()) break;

                for (ConsumerRecord<byte[], byte[]> r : records) {
                    out.add(toDto(r));
                    if (out.size() >= target) break;
                }
            }

            return new ConsumerDto.FetchResponse(clusterName, topic, out.size(), out);

        } catch (Exception e) {
            log.error("Consumer fetch failed. cluster={} topic={} bootstrap={}", clusterName, topic, bootstrap, e);
            throw e;
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private List<TopicPartition> resolveTopicPartitions(
            KafkaConsumer<byte[], byte[]> consumer,
            String topic,
            List<Integer> partitions
    ) {
        if (partitions != null && !partitions.isEmpty()) {
            List<TopicPartition> tps = new ArrayList<>();
            for (Integer p : partitions) {
                if (p != null && p >= 0) tps.add(new TopicPartition(topic, p));
            }
            return tps;
        }

        var parts = consumer.partitionsFor(topic);
        if (parts == null) return List.of();

        return parts.stream()
                .map(pi -> new TopicPartition(topic, pi.partition()))
                .toList();
    }

    private void applyStartPosition(
            KafkaConsumer<byte[], byte[]> consumer,
            List<TopicPartition> tps,
            ConsumerDto.FetchRequest req
    ) {
        ConsumerDto.Position pos = req.position();
        if (pos == null) pos = ConsumerDto.Position.LATEST;

        switch (pos) {
            case EARLIEST -> consumer.seekToBeginning(tps);
            case LATEST -> consumer.seekToEnd(tps);

            case OFFSET -> {
                Long off = req.offset();
                if (off == null || off < 0) {
                    consumer.seekToEnd(tps);
                    return;
                }
                for (TopicPartition tp : tps) consumer.seek(tp, off);
            }

            case TIMESTAMP -> {
                Long ts = req.timestampMs();
                if (ts == null || ts <= 0) {
                    consumer.seekToEnd(tps);
                    return;
                }
                Map<TopicPartition, Long> query = new HashMap<>();
                for (TopicPartition tp : tps) query.put(tp, ts);

                var offsets = consumer.offsetsForTimes(query);
                for (TopicPartition tp : tps) {
                    var ot = offsets.get(tp);
                    if (ot != null) consumer.seek(tp, ot.offset());
                    else consumer.seekToEnd(List.of(tp));
                }
            }
        }
    }

    private ConsumerRecordDto toDto(ConsumerRecord<byte[], byte[]> r) {

        String key = (r.key() == null) ? null : B64.encodeToString(r.key());
        String value = (r.value() == null) ? null : B64.encodeToString(r.value());

        String headers = headersToString(r.headers());
        Integer sizeBytes = (r.value() == null) ? null : r.value().length;

        return new ConsumerRecordDto(
                r.partition(),
                r.offset(),
                r.timestamp(),
                key,
                value,
                headers,
                sizeBytes
        );
    }

    private String headersToString(Iterable<Header> headers) {
        if (headers == null) return null;

        StringBuilder sb = new StringBuilder();
        for (Header h : headers) {
            if (h == null) continue;
            if (sb.length() > 0) sb.append("; ");
            sb.append(h.key()).append("=")
                    .append(h.value() == null ? "null" : B64.encodeToString(h.value()));
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private int safeMax(Integer max) {
        int v = (max == null) ? 50 : max;
        if (v < 1) v = 1;
        if (v > 1000) v = 1000;
        return v;
    }

    private int safePollTimeoutMs(Integer ms) {
        int v = (ms == null) ? 1500 : ms;
        if (v < 100) v = 100;
        if (v > 30000) v = 30000;
        return v;
    }

    private long safeTimeoutMs() {
        Integer ms = props.getDefaultApiTimeoutMs();
        return (ms == null || ms < 1000) ? 15000L : ms.longValue();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
