package com.srikar.kafka.model;

// No annotation required for Spring Boot (Jackson handles JSON automatically)
public class BrokerSnapshotRow {

    private String serverName;
    private short  nodeId;
    private String role;
    private boolean online;
    private String serverAddress;
    private String lastCheckedAt;
    private String source;

    public BrokerSnapshotRow() {
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public short getNodeId() {
        return nodeId;
    }

    public void setNodeId(short nodeId) {
        this.nodeId = nodeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public String getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(String lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
