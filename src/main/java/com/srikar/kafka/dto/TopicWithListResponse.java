package com.srikar.kafka.dto;

import com.srikar.kafka.domain.TopicActionResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TopicWithListResponse {

    private TopicActionResult action;  // result of create/delete
    private String cluster;
    private int count;
    private List<String> topics;

}
