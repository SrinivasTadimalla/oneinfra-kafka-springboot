// src/main/java/com/srikar/kafka/dto/schema/SchemaDetailDto.java
package com.srikar.kafka.dto.schema;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaDetailDto {

    private SchemaSubjectDto subject;

    /**
     * Newest first (recommended for UI)
     */
    private List<SchemaVersionDto> versions;

    /**
     * Optional: only include when "includeText=true" OR for a single version fetch.
     */
    private String schemaText;
}
