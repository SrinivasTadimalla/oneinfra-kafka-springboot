package com.srikar.kafka.dto.schema;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaSubjectSummaryDto {

    private UUID subjectId;
    private UUID clusterId;

    private String subject;

    private SchemaType schemaType;
    private CompatibilityMode compatibility;

    private Integer latestVersion;

    private boolean enabled;
    private Instant updatedAt;
}
