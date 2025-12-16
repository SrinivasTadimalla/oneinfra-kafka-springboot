package com.srikar.kafka.service;

import com.srikar.kafka.model.BrokerSnapshotRow;
import com.srikar.kafka.repository.BrokerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ClusterService {

    private final BrokerRepository repo;

    public ClusterService(BrokerRepository repo) {
        this.repo = repo;
    }

    /** Fast read using projection; no entity hydration. */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Map<String, Object>> snapshot(String clusterName) {
        List<BrokerSnapshotRow> rows = repo.snapshot(clusterName);
        return rows.stream()
                .map(s -> Map.<String, Object>of(
                        "serverName",    s.getServerName(),
                        "nodeId",        s.getNodeId(),
                        "role",          s.getRole(),
                        "online",        s.isOnline(),
                        "serverAddress", s.getServerAddress(),
                        "lastCheckedAt", s.getLastCheckedAt(),
                        "source",        s.getSource()
                ))
                .collect(Collectors.toList());
    }

    /** Transactional write path used by controller. */
    @Transactional
    public void recordNodeStatusTx(String serverName,
                                   short nodeId,
                                   boolean online,
                                   String source,
                                   String logs,
                                   String actor /* unused now */) {
        // Repository signature: (serverName, nodeId, online, source, logs)
        repo.recordNodeStatus(serverName, nodeId, online, source, logs);
    }
}
