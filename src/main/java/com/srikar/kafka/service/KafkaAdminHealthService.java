package com.srikar.kafka.service;

import com.srikar.kafka.config.KafkaAdminClientFactory;
import com.srikar.kafka.config.KafkaAdminProperties;
import com.srikar.kafka.dto.cluster.KafkaClusterHealthDto;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaAdminHealthService {

    private final KafkaAdminClientFactory adminFactory;
    private final KafkaAdminProperties props;

    public KafkaClusterHealthDto probe(String bootstrapServers) {
        Instant observedAt = Instant.now();

        long timeoutMs = props.getDefaultApiTimeoutMs();

        try (AdminClient admin = adminFactory.create(bootstrapServers)) {

            var desc = admin.describeCluster();

            String clusterId = desc.clusterId().get(timeoutMs, TimeUnit.MILLISECONDS);

            Node controller = desc.controller().get(timeoutMs, TimeUnit.MILLISECONDS);
            String controllerStr = controller != null ? (controller.host() + ":" + controller.port()) : null;

            Collection<Node> nodes = desc.nodes().get(timeoutMs, TimeUnit.MILLISECONDS);
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
