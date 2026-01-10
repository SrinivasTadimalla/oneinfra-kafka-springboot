package com.srikar.kafka.dto.consumer;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumerRecordDto {

    private int partition;
    private long offset;
    private long timestamp;

    // base64 (safe for binary) - you can decode in UI
    private String key;
    private String value;

    // simple header representation: "k=v; k2=v2"
    private String headers;

    private Integer sizeBytes;

    public ConsumerRecordDto() {}

    public ConsumerRecordDto(
            int partition,
            long offset,
            long timestamp,
            String key,
            String value,
            String headers,
            Integer sizeBytes
    ) {
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.key = key;
        this.value = value;
        this.headers = headers;
        this.sizeBytes = sizeBytes;
    }

    public int getPartition() { return partition; }
    public long getOffset() { return offset; }
    public long getTimestamp() { return timestamp; }
    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getHeaders() { return headers; }
    public Integer getSizeBytes() { return sizeBytes; }

    public void setPartition(int partition) { this.partition = partition; }
    public void setOffset(long offset) { this.offset = offset; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setKey(String key) { this.key = key; }
    public void setValue(String value) { this.value = value; }
    public void setHeaders(String headers) { this.headers = headers; }
    public void setSizeBytes(Integer sizeBytes) { this.sizeBytes = sizeBytes; }
}
