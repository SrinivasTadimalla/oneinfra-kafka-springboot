package com.srikar.kafka.service;

import com.srikar.kafka.dto.KafkaClusterHealthDto;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaAdminHealthService {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    public KafkaClusterHealthDto probe(String bootstrapServers) {
        Instant observedAt = Instant.now();

        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) TIMEOUT.toMillis());

        try (AdminClient admin = AdminClient.create(props)) {
            var desc = admin.describeCluster();

            String clusterId = desc.clusterId().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            Node controller = desc.controller().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            String controllerStr = controller != null ? (controller.host() + ":" + controller.port()) : null;

            Collection<Node> nodes = desc.nodes().get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            List<String> brokers = (nodes == null) ? List.of()
                    : nodes.stream()
                    .map(n -> n.host() + ":" + n.port())
                    .sorted()
                    .toList();

            return KafkaClusterHealthDto.builder()
                    .status("UP")
                    .clusterId(clusterId)
                    .controller(controllerStr)
                    .brokerCount(brokers.size())
                    .brokers(brokers)
                    .observedAt(observedAt)
                    .error(null)
                    .build();

        } catch (Exception ex) {
            return KafkaClusterHealthDto.builder()
                    .status("DOWN")
                    .clusterId(null)
                    .controller(null)
                    .brokerCount(0)
                    .brokers(List.of())
                    .observedAt(observedAt)
                    .error(shortError(ex))
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
