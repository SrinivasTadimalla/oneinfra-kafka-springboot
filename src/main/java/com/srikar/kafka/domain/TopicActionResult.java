package com.srikar.kafka.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopicActionResult {

    private boolean ok;
    private String topicName;
    private String action;   // "create" or "delete"
    private String message;

}
