package com.srikar.kafka.bootstrap;

import com.srikar.kafka.db.KafkaClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KafkaBootstrapResolver {

    private final StringRedisTemplate redis;
    private final KafkaClusterRepository repo;

    private static final String KEY_PREFIX = "oneinfra:kafka:bootstrap:";
    private static final Duration TTL = Duration.ofMinutes(10);

    public String resolve(String clusterName) {
        String key = KEY_PREFIX + clusterName;

        String cached = redis.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) return cached;

        var entity = repo.findByNameIgnoreCase(clusterName)
                .orElseThrow(() -> new IllegalArgumentException("Unknown cluster: " + clusterName));

        redis.opsForValue().set(key, entity.getBootstrapServers(), TTL);
        return entity.getBootstrapServers();
    }

}
