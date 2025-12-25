package com.srikar.kafka.service;

import com.srikar.kafka.dto.KafkaClusterHealthDto;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaAdminHealthService {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    public KafkaClusterHealthDto probe(String bootstrapServers) {
        long start = System.currentTimeMillis();

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());

        try (AdminClient admin = AdminClient.create(props)) {
            var desc = admin.describeCluster();

            String clusterId = desc.clusterId().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Node controller = desc.controller().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            Collection<Node> nodes = desc.nodes().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            long latency = System.currentTimeMillis() - start;

            return KafkaClusterHealthDto.builder()
                    .status(KafkaClusterHealthDto.Status.UP)
                    .clusterId(clusterId)
                    .controller(controller != null ? controller.host() + ":" + controller.port() : null)
                    .brokerCount(nodes != null ? nodes.size() : 0)
                    .latencyMs(latency)
                    .build();

        } catch (Exception ex) {
            long latency = System.currentTimeMillis() - start;

            return KafkaClusterHealthDto.builder()
                    .status(KafkaClusterHealthDto.Status.DOWN)
                    .error(shortError(ex))
                    .latencyMs(latency)
                    .build();
        }
    }

    private String shortError(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        String msg = root.getMessage();
        if (msg == null || msg.isBlank()) msg = root.getClass().getSimpleName();
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
