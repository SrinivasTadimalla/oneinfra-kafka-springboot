// src/main/java/com/srikar/kafka/dto/schema/SchemaQueryRequest.java
package com.srikar.kafka.dto.schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaQueryRequest {

    @NotNull
    private UUID clusterId;

    @NotBlank
    private String topicName;

    @NotNull
    private SchemaPart part; // KEY / VALUE
}
