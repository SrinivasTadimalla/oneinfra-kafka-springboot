package com.srikar.kafka.model;

/**
 * Lightweight projection for /cluster/status rows
 * (works with Spring Data native queries as an interface-based projection).
 */
public interface BrokerSnapshot {
    String  getServerName();
    short   getNodeId();
    String  getRole();
    boolean isOnline();
    String  getServerAddress();
    String  getLastCheckedAt();  // formatted string from SQL
    String  getSource();
}
