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
        private String subject;   // schema name

        @NotNull
        private SchemaType schemaType;

        @NotBlank
        private String schemaText;

        private CompatibilityMode compatibility;

}
