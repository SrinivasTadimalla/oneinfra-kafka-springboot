// src/main/java/com/srikar/kafka/dto/schema/SchemaRegisterRequest.java
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
public class SchemaRegisterRequest {

    @NotNull
    private UUID clusterId;

    @NotBlank
    private String topicName;

    @NotNull
    private SchemaPart part; // KEY or VALUE

    @NotNull
    private SchemaType schemaType; // AVRO (for now)

    @NotBlank
    private String schemaText; // full Avro JSON schema string

    /**
     * Optional override. If null -> backend uses default (e.g., BACKWARD).
     */
    private CompatibilityMode compatibility;

}
