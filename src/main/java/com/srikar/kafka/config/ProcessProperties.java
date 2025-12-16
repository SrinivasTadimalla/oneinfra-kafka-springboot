package com.srikar.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "process")
public class ProcessProperties {

    private boolean enabled = true;
    private Ssh ssh = new Ssh();
    private Services services = new Services();
    private Hosts hosts = new Hosts();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Ssh getSsh() { return ssh; }
    public void setSsh(Ssh ssh) { this.ssh = ssh; }

    public Services getServices() { return services; }
    public void setServices(Services services) { this.services = services; }

    public Hosts getHosts() { return hosts; }
    public void setHosts(Hosts hosts) { this.hosts = hosts; }

    // ---------- Nested Classes ----------

    public static class Ssh {
        private String user;
        private int port = 22;
        private String privateKeyPath = "/var/run/secrets/ssh/id_rsa";
        private String knownHostsPath = "/var/run/secrets/ssh/known_hosts";
        private int connectTimeoutMs = 3000;
        private int commandTimeoutMs = 15000;

        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getPrivateKeyPath() { return privateKeyPath; }
        public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
        public String getKnownHostsPath() { return knownHostsPath; }
        public void setKnownHostsPath(String knownHostsPath) { this.knownHostsPath = knownHostsPath; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getCommandTimeoutMs() { return commandTimeoutMs; }
        public void setCommandTimeoutMs(int commandTimeoutMs) { this.commandTimeoutMs = commandTimeoutMs; }
    }

    public static class Services {
        private String brokerUnit = "kafka-broker";
        private String controllerUnit = "kafka-controller";

        public String getBrokerUnit() { return brokerUnit; }
        public void setBrokerUnit(String brokerUnit) { this.brokerUnit = brokerUnit; }
        public String getControllerUnit() { return controllerUnit; }
        public void setControllerUnit(String controllerUnit) { this.controllerUnit = controllerUnit; }
    }

    public static class Hosts {
        private String controller;            // e.g. 10.32.45.20
        private Map<String, String> brokers;  // e.g. { "2": "10.32.45.39", "3": "10.32.45.175" }

        public String getController() { return controller; }
        public void setController(String controller) { this.controller = controller; }
        public Map<String, String> getBrokers() { return brokers; }
        public void setBrokers(Map<String, String> brokers) { this.brokers = brokers; }

        public String getControllerResolved() { return controller; }
    }
}
