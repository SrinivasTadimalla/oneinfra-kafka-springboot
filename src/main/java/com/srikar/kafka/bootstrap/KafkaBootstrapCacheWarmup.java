package com.srikar.kafka.bootstrap;

import com.srikar.kafka.db.KafkaClusterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBootstrapCacheWarmup implements ApplicationRunner {

    private final KafkaClusterRepository repo;
    private final StringRedisTemplate redis;

    private static final String KEY_PREFIX = "oneinfra:kafka:bootstrap:";
    private static final Duration TTL = Duration.ofMinutes(10);

    @Override
    public void run(ApplicationArguments args) {
        var clusters = repo.findAllByOrderByNameAsc();

        for (var c : clusters) {
            String key = KEY_PREFIX + c.getName();
            String value = c.getBootstrapServers(); // ✅ from DB only
            redis.opsForValue().set(key, value, TTL);
            log.info("Cached bootstrap servers: {} -> {}", key, value);
        }
    }
}
