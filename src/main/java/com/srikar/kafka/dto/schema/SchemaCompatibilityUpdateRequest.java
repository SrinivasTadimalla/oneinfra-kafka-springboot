// src/main/java/com/srikar/kafka/dto/schema/SchemaCompatibilityUpdateRequest.java
package com.srikar.kafka.dto.schema;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaCompatibilityUpdateRequest {

    @NotNull
    private CompatibilityMode compatibility;
}
