package com.srikar.kafka.service;

import com.srikar.kafka.dto.KafkaClusterHealthDto;
import com.srikar.kafka.dto.KafkaClusterMetaDto;
import com.srikar.kafka.dto.KafkaClusterOverviewDto;
import com.srikar.kafka.entity.KafkaClusterEntity;
import com.srikar.kafka.db.KafkaClusterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaClusterSnapshotService {

    private final KafkaClusterRepository clusterRepo;
    private final KafkaAdminHealthService healthService;

    /**
     * Returns:
     * - DB cluster metadata
     * - Live Kafka health (AdminClient)
     */
    public List<KafkaClusterOverviewDto> listClustersWithHealth() {

        List<KafkaClusterEntity> clusters =
                clusterRepo.findAllByOrderByNameAsc();

        return clusters.stream()
                .map(cluster -> {

                    KafkaClusterMetaDto meta = toMeta(cluster);

                    KafkaClusterHealthDto health =
                            meta.isEnabled()
                                    ? healthService.probe(meta.getBootstrapServers())
                                    : disabledHealth();

                    return KafkaClusterOverviewDto.builder()
                            .meta(meta)
                            .health(health)
                            .build();
                })
                .toList();
    }

    /**
     * DB → API meta mapping
     */
    private KafkaClusterMetaDto toMeta(KafkaClusterEntity e) {
        return KafkaClusterMetaDto.builder()
                .name(e.getName())
                .environment(e.getEnvironment()) // nullable by design
                .bootstrapServers(e.getBootstrapServers())
                .enabled(e.isEnabled())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    /**
     * Health object for disabled clusters
     */
    private KafkaClusterHealthDto disabledHealth() {
        return KafkaClusterHealthDto.builder()
                .status("UNKNOWN")
                .observedAt(Instant.now())
                .brokerCount(0)
                .brokers(List.of())
                .error("Cluster disabled in DB")
                .build();
    }
}
