package com.srikar.kafka.service;

import com.srikar.kafka.config.KafkaAdminClientFactory;
import com.srikar.kafka.db.KafkaClusterRepository;
import com.srikar.kafka.dto.consumer.ConsumerGroupDetailDto;
import com.srikar.kafka.dto.consumer.ConsumerGroupPartitionLagDto;
import com.srikar.kafka.dto.consumer.ConsumerGroupSummaryDto;
import com.srikar.kafka.entity.KafkaClusterEntity;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class KafkaConsumerGroupsService {

    private final KafkaClusterRepository clusterRepository;
    private final KafkaAdminClientFactory adminFactory;

    private AdminClient adminForCluster(UUID clusterId) {
        KafkaClusterEntity cluster = clusterRepository.findById(clusterId)
                .orElseThrow(() -> new IllegalArgumentException("Cluster not found: " + clusterId));

        if (!cluster.isEnabled()) {
            throw new IllegalStateException("Cluster is disabled: " + cluster.getName() + " (" + clusterId + ")");
        }

        String bootstrap = cluster.getBootstrapServers() == null ? "" : cluster.getBootstrapServers().trim();
        if (bootstrap.isBlank()) {
            throw new IllegalStateException("bootstrapServers is empty for cluster: " + cluster.getName() + " (" + clusterId + ")");
        }

        return adminFactory.create(bootstrap);
    }

    public List<ConsumerGroupSummaryDto> listGroups(UUID clusterId) {
        try (AdminClient admin = adminForCluster(clusterId)) {

            Collection<ConsumerGroupListing> listings =
                    admin.listConsumerGroups(new ListConsumerGroupsOptions().timeoutMs(15_000))
                            .all()
                            .get(15, TimeUnit.SECONDS);

            List<String> groupIds = listings.stream()
                    .map(ConsumerGroupListing::groupId)
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();

            if (groupIds.isEmpty()) return List.of();

            Map<String, ConsumerGroupDescription> descMap =
                    admin.describeConsumerGroups(groupIds, new DescribeConsumerGroupsOptions().timeoutMs(15_000))
                            .all()
                            .get(15, TimeUnit.SECONDS);

            List<ConsumerGroupSummaryDto> out = new ArrayList<>(groupIds.size());

            for (String gid : groupIds) {
                ConsumerGroupDescription d = descMap.get(gid);
                if (d == null) continue;

                GroupOffsetsAndLag lag = computeLag(admin, gid);

                out.add(ConsumerGroupSummaryDto.builder()
                        .groupId(gid)
                        .state(mapState(d.state()))
                        .membersCount(d.members() == null ? 0 : d.members().size())
                        .topicsCount(lag.topicsCount())
                        .totalLag(lag.totalLag())
                        .build());
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("Failed to list consumer groups: " + e.getMessage(), e);
        }
    }

    public ConsumerGroupDetailDto getGroupDetail(UUID clusterId, String groupId) {
        try (AdminClient admin = adminForCluster(clusterId)) {

            Map<String, ConsumerGroupDescription> map =
                    admin.describeConsumerGroups(List.of(groupId), new DescribeConsumerGroupsOptions().timeoutMs(15_000))
                            .all()
                            .get(15, TimeUnit.SECONDS);

            ConsumerGroupDescription d = map.get(groupId);
            if (d == null) throw new NoSuchElementException("Group not found: " + groupId);

            GroupOffsetsAndLag lag = computeLag(admin, groupId);

            List<String> clientIds = d.members() == null ? List.of() :
                    d.members().stream()
                            .map(m -> m.clientId() == null ? "" : m.clientId())
                            .filter(s -> !s.isBlank())
                            .distinct()
                            .sorted()
                            .toList();

            return ConsumerGroupDetailDto.builder()
                    .groupId(groupId)
                    .state(mapState(d.state()))
                    .membersCount(d.members() == null ? 0 : d.members().size())
                    .memberClientIds(clientIds)
                    .topicsCount(lag.topicsCount())
                    .totalLag(lag.totalLag())
                    .partitions(lag.partitions())
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to read group details: " + e.getMessage(), e);
        }
    }

    private String mapState(ConsumerGroupState state) {
        return state == null ? "UNKNOWN" : state.name();
    }

    private record GroupOffsetsAndLag(
            int topicsCount,
            long totalLag,
            List<ConsumerGroupPartitionLagDto> partitions
    ) {}

    private GroupOffsetsAndLag computeLag(AdminClient admin, String groupId) throws Exception {

        Map<TopicPartition, OffsetAndMetadata> committed =
                admin.listConsumerGroupOffsets(groupId, new ListConsumerGroupOffsetsOptions().timeoutMs(15_000))
                        .partitionsToOffsetAndMetadata()
                        .get(15, TimeUnit.SECONDS);

        if (committed == null || committed.isEmpty()) {
            return new GroupOffsetsAndLag(0, 0L, List.of());
        }

        Map<TopicPartition, OffsetSpec> req = new HashMap<>();
        for (TopicPartition tp : committed.keySet()) {
            req.put(tp, OffsetSpec.latest());
        }

        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> end =
                admin.listOffsets(req, new ListOffsetsOptions().timeoutMs(15_000))
                        .all()
                        .get(15, TimeUnit.SECONDS);

        long totalLag = 0L;
        List<ConsumerGroupPartitionLagDto> rows = new ArrayList<>(committed.size());

        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : committed.entrySet()) {
            TopicPartition tp = e.getKey();

            long committedOffset = e.getValue() == null ? -1L : e.getValue().offset();
            long endOffset = end.get(tp) == null ? -1L : end.get(tp).offset();

            long lag = 0L;
            if (committedOffset >= 0 && endOffset >= 0) {
                lag = Math.max(0L, endOffset - committedOffset);
            }

            totalLag += lag;

            rows.add(ConsumerGroupPartitionLagDto.builder()
                    .topic(tp.topic())
                    .partition(tp.partition())
                    .committedOffset(committedOffset)
                    .endOffset(endOffset)
                    .lag(lag)
                    .build());
        }

        int topicsCount = (int) committed.keySet().stream()
                .map(TopicPartition::topic)
                .distinct()
                .count();

        rows.sort((a, b) -> Long.compare(b.getLag(), a.getLag()));

        return new GroupOffsetsAndLag(topicsCount, totalLag, rows);
    }
}
