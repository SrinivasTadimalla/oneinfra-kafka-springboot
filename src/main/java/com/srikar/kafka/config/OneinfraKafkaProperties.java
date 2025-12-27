package com.srikar.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "oneinfra.kafka")
public class OneinfraKafkaProperties {

    private Admin admin = new Admin();
    private Ssl ssl = new Ssl();

    @Data
    public static class Admin {
        private int requestTimeoutMs = 15000;
        private int defaultApiTimeoutMs = 15000;

        private int socketSetupTimeoutMs = 10000;
        private int socketSetupTimeoutMaxMs = 30000;

        private List<String> bootstrapServers = new ArrayList<>();
        private String clientId = "oneinfra-kafka-admin";
    }

    @Data
    public static class Ssl {
        private String securityProtocol = "SSL";

        private String truststoreLocation;
        private String truststorePassword;
        private String truststoreType = "PKCS12";

        private String keystoreLocation;
        private String keystorePassword;
        private String keystoreType = "PKCS12";

        private String keyPassword;

        /** empty disables hostname verification (IP-based clusters) */
        private String endpointIdentificationAlgorithm = "";
    }
}
