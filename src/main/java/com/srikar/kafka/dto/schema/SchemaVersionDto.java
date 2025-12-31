// src/main/java/com/srikar/kafka/dto/schema/SchemaVersionDto.java
package com.srikar.kafka.dto.schema;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaVersionDto {

    private UUID id;

    private UUID clusterId;

    private UUID subjectId;
    private String subject;

    private Integer version;

    private SchemaType schemaType;
    private CompatibilityMode compatibility;

    private String schemaCanonical;
    private String schemaRaw;
    private String schemaHash;

    private boolean enabled;
    private Instant createdAt;
}
