package com.srikar.kafka.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class KafkaKraftJmxHealthService {

    public Result probe(String host, int port, int timeoutMs) {
        String observedAt = Instant.now().toString();
        String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";

        Map<String, Object> env = new HashMap<>();

        try (JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url), env)) {
            MBeanServerConnection mbean = connector.getMBeanServerConnection();

            // 1) Quick sanity: how many kafka.* mbeans exist?
            Set<ObjectName> kafkaAll = mbean.queryNames(new ObjectName("kafka.*:*"), null);

            // 2) KRaft signals: metadata quorum / raft metrics / controller
            Set<ObjectName> quorum = mbean.queryNames(new ObjectName("kafka.server:type=metadata-quorum,*"), null);
            Set<ObjectName> raft = mbean.queryNames(new ObjectName("kafka.server:type=raft-metrics,*"), null);
            Set<ObjectName> controller = mbean.queryNames(new ObjectName("kafka.controller:*"), null);

            boolean up = !(quorum.isEmpty() && raft.isEmpty() && controller.isEmpty());

            if (up) {
                String details =
                        "kafkaMBeans=" + kafkaAll.size() +
                                ", metadataQuorum=" + quorum.size() +
                                ", raftMetrics=" + raft.size() +
                                ", controller=" + controller.size();

                return Result.builder()
                        .status("UP")
                        .observedAt(observedAt)
                        .details(details)
                        .build();
            }

            // If JMX is reachable but no expected kafka mbeans -> return DOWN with info
            return Result.builder()
                    .status("DOWN")
                    .observedAt(observedAt)
                    .error("JMX reachable but no KRaft MBeans found. kafkaMBeans=" + kafkaAll.size())
                    .build();

        } catch (Exception e) {
            return Result.builder()
                    .status("DOWN")
                    .observedAt(observedAt)
                    .error(e.getClass().getSimpleName() + ": " + safeMsg(e))
                    .build();
        }
    }

    private static String safeMsg(Throwable t) {
        String m = t.getMessage();
        if (m == null) return "";
        return m.length() > 300 ? m.substring(0, 300) : m;
    }

    @Getter
    @Builder
    public static class Result {
        private String status;
        private String observedAt;
        private String details;
        private String error;
    }
}
