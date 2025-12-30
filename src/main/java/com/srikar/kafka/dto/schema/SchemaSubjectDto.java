// src/main/java/com/srikar/kafka/dto/schema/SchemaSubjectDto.java
package com.srikar.kafka.dto.schema;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSubjectDto {

    private UUID id;
    private UUID clusterId;

    private String topicName;
    private SchemaPart part;

    /**
     * Convention: <topic>-value or <topic>-key
     */
    private String subject;

    private SchemaType schemaType;
    private CompatibilityMode compatibility;

    private Instant createdAt;
    private Instant updatedAt;
}
