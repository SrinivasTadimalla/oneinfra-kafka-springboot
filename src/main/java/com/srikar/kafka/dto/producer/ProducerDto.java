package com.srikar.kafka.dto.producer;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public final class ProducerDto {

    private ProducerDto() {}

    // ----------------------------
    // Schema Types (UI already uses these)
    // ----------------------------
    public enum SchemaType {
        AVRO,
        JSON,
        PROTOBUF
    }

    // ----------------------------
    // Schema Reference
    // ----------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SchemaRef {
        private SchemaType schemaType;   // AVRO/JSON/PROTOBUF
        private String subject;          // e.g. sale-event-value
        private Integer version;         // null => latest

        public SchemaRef() {}

        public SchemaRef(SchemaType schemaType, String subject, Integer version) {
            this.schemaType = schemaType;
            this.subject = subject;
            this.version = version;
        }

        public SchemaType getSchemaType() { return schemaType; }
        public String getSubject() { return subject; }
        public Integer getVersion() { return version; }

        public void setSchemaType(SchemaType schemaType) { this.schemaType = schemaType; }
        public void setSubject(String subject) { this.subject = subject; }
        public void setVersion(Integer version) { this.version = version; }
    }

    // ----------------------------
    // Validation Error
    // ----------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        private String path;
        private String message;

        public ValidationError() {}

        public ValidationError(String path, String message) {
            this.path = path;
            this.message = message;
        }

        public String getPath() { return path; }
        public String getMessage() { return message; }

        public void setPath(String path) { this.path = path; }
        public void setMessage(String message) { this.message = message; }
    }

    // ----------------------------
    // Validate Request / Response
    // ----------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidateRequest {
        private String clusterName;
        private String topicName;
        private String payload;        // raw JSON string
        private SchemaRef schemaRef;   // required in schema mode

        public ValidateRequest() {}

        public ValidateRequest(String clusterName, String topicName, String payload, SchemaRef schemaRef) {
            this.clusterName = clusterName;
            this.topicName = topicName;
            this.payload = payload;
            this.schemaRef = schemaRef;
        }

        public String getClusterName() { return clusterName; }
        public String getTopicName() { return topicName; }
        public String getPayload() { return payload; }
        public SchemaRef getSchemaRef() { return schemaRef; }

        public void setClusterName(String clusterName) { this.clusterName = clusterName; }
        public void setTopicName(String topicName) { this.topicName = topicName; }
        public void setPayload(String payload) { this.payload = payload; }
        public void setSchemaRef(SchemaRef schemaRef) { this.schemaRef = schemaRef; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidateResponse {
        private boolean valid;
        private Integer resolvedSchemaVersion;
        private List<ValidationError> errors;

        public ValidateResponse() {}

        public ValidateResponse(boolean valid, Integer resolvedSchemaVersion, List<ValidationError> errors) {
            this.valid = valid;
            this.resolvedSchemaVersion = resolvedSchemaVersion;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public Integer getResolvedSchemaVersion() { return resolvedSchemaVersion; }
        public List<ValidationError> getErrors() { return errors; }

        public void setValid(boolean valid) { this.valid = valid; }
        public void setResolvedSchemaVersion(Integer resolvedSchemaVersion) { this.resolvedSchemaVersion = resolvedSchemaVersion; }
        public void setErrors(List<ValidationError> errors) { this.errors = errors; }
    }

    // ----------------------------
    // Publish Request / Response
    // ----------------------------
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublishRequest {
        private String clusterName;
        private String topicName;

        // optional key (text). if null => keyless publish
        private String key;

        // required payload string (UI sends JSON or text)
        private String payload;

        // optional schema reference (schema-mode)
        private SchemaRef schemaRef;

        public PublishRequest() {}

        public PublishRequest(String clusterName, String topicName, String key, String payload, SchemaRef schemaRef) {
            this.clusterName = clusterName;
            this.topicName = topicName;
            this.key = key;
            this.payload = payload;
            this.schemaRef = schemaRef;
        }

        public String getClusterName() { return clusterName; }
        public String getTopicName() { return topicName; }
        public String getKey() { return key; }
        public String getPayload() { return payload; }
        public SchemaRef getSchemaRef() { return schemaRef; }

        public void setClusterName(String clusterName) { this.clusterName = clusterName; }
        public void setTopicName(String topicName) { this.topicName = topicName; }
        public void setKey(String key) { this.key = key; }
        public void setPayload(String payload) { this.payload = payload; }
        public void setSchemaRef(SchemaRef schemaRef) { this.schemaRef = schemaRef; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PublishResponse {
        private String topic;
        private Integer partition;
        private Long offset;
        private Long timestamp;

        public PublishResponse() {}

        public PublishResponse(String topic, Integer partition, Long offset, Long timestamp) {
            this.topic = topic;
            this.partition = partition;
            this.offset = offset;
            this.timestamp = timestamp;
        }

        public String getTopic() { return topic; }
        public Integer getPartition() { return partition; }
        public Long getOffset() { return offset; }
        public Long getTimestamp() { return timestamp; }

        public void setTopic(String topic) { this.topic = topic; }
        public void setPartition(Integer partition) { this.partition = partition; }
        public void setOffset(Long offset) { this.offset = offset; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
