package com.srikar.kafka.service;

import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.entity.KafkaClusterEntity;
import com.srikar.kafka.entity.KafkaNodeEntity;
import com.srikar.kafka.dto.UpsertKafkaClusterRequest;
import com.srikar.kafka.dto.UpsertKafkaNodeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KafkaClusterCrudService {

    private final KafkaClusterRepository clusterRepo;

    /**
     * Upsert (create or update) Kafka cluster metadata + its nodes.
     *
     * Strategy:
     * - Find cluster by unique name
     * - If not exists -> create
     * - Replace nodes list (orphanRemoval=true will delete old nodes)
     */
    @Transactional
    public KafkaClusterEntity upsert(UpsertKafkaClusterRequest req) {

        validate(req);

        KafkaClusterEntity cluster = clusterRepo.findByName(req.getName())
                .orElseGet(() -> KafkaClusterEntity.builder().name(req.getName()).build());

        cluster.setMode(req.getMode());
        cluster.setBootstrapServers(req.getBootstrapServers());

        // Replace nodes safely
        cluster.getNodes().clear();

        if (req.getNodes() != null) {
            for (UpsertKafkaNodeRequest n : req.getNodes()) {
                cluster.addNode(toEntity(n));
            }
        }

        return clusterRepo.save(cluster);
    }

    private KafkaNodeEntity toEntity(UpsertKafkaNodeRequest n) {
        return KafkaNodeEntity.builder()
                .nodeId(n.getNodeId())
                .host(n.getHost())
                .port(n.getPort())
                .role(n.getRole())
                .isVm(n.isVm())
                .vmName(n.getVmName())
                .observedAt(null) // runtime probe can fill later
                .build();
    }

    private void validate(UpsertKafkaClusterRequest req) {
        if (req == null) throw new IllegalArgumentException("Request body is required");

        if (isBlank(req.getName())) throw new IllegalArgumentException("name is required");
        if (isBlank(req.getMode())) throw new IllegalArgumentException("mode is required");
        if (isBlank(req.getBootstrapServers())) throw new IllegalArgumentException("bootstrapServers is required");

        // Minimum validation for nodes
        if (req.getNodes() == null || req.getNodes().isEmpty()) {
            throw new IllegalArgumentException("At least one node is required (controller/broker)");
        }

        // Ensure every node has host+port+role
        for (var n : req.getNodes()) {
            if (n == null) throw new IllegalArgumentException("nodes contains null item");
            if (isBlank(n.getHost())) throw new IllegalArgumentException("node.host is required");
            if (n.getPort() == null) throw new IllegalArgumentException("node.port is required");
            if (isBlank(n.getRole())) throw new IllegalArgumentException("node.role is required");
        }

        // Ensure at least one controller and one broker role exist (simple string check)
        boolean hasController = req.getNodes().stream()
                .filter(Objects::nonNull)
                .map(UpsertKafkaNodeRequest::getRole)
                .filter(r -> r != null)
                .anyMatch(r -> r.equalsIgnoreCase("CONTROLLER") || r.equalsIgnoreCase("BOTH"));

        boolean hasBroker = req.getNodes().stream()
                .filter(Objects::nonNull)
                .map(UpsertKafkaNodeRequest::getRole)
                .filter(r -> r != null)
                .anyMatch(r -> r.equalsIgnoreCase("BROKER") || r.equalsIgnoreCase("BOTH"));

        if (!hasController) throw new IllegalArgumentException("At least one CONTROLLER (or BOTH) node is required");
        if (!hasBroker) throw new IllegalArgumentException("At least one BROKER (or BOTH) node is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
