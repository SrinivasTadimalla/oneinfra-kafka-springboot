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
    private UUID subjectId;

    private Integer version;
    private String fingerprint;

    private SchemaVersionStatus status;

    private Instant createdAt;
}
