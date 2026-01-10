package com.srikar.kafka.utilities;

import com.srikar.kafka.enums.KafkaTopicStatus;
import lombok.*;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicKafkaDelta {

    // ----------------------------
    // Kafka-side deltas
    // ----------------------------
    private boolean kafkaChange;

    private Integer partitionsIncreaseTo;

    @Builder.Default
    private Map<String, String> configDelta = Collections.emptyMap();

    // ----------------------------
    // DB-side deltas
    // ----------------------------
    private boolean dbChange;

    private String description;

    private Boolean enabled;

    private KafkaTopicStatus status;

    public boolean hasAnyChange() {
        return kafkaChange || dbChange;
    }

}
