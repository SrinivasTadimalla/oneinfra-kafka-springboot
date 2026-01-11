package com.srikar.kafka.dto.consumer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupRegisterRequest {

    @NotNull
    private UUID clusterId;

    @NotBlank
    private String groupId;

    // Optional metadata for your UI / governance
    private String description;
    private String customerName;

}
