package com.srikar.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "oneinfra.kafka.admin")
public class KafkaAdminProperties {

    /**
     * Optional fallback bootstraps (only used if code calls factory.create(null/blank))
     * Example: ["192.168.66.108:9093", "192.168.66.115:9093"]
     */
    private List<String> bootstrapServers = new ArrayList<>();

    /** AdminClient client.id */
    private String clientId = "oneinfra-kafka-admin";

    // ---- Admin timeouts ----
    private Integer requestTimeoutMs = 15000;
    private Integer defaultApiTimeoutMs = 15000;

    // ---- TLS handshake / initial connect helpers ----
    private Integer socketSetupTimeoutMs = 10000;
    private Integer socketSetupTimeoutMaxMs = 30000;

    // ---- SSL / mTLS ----
    private Ssl ssl = new Ssl();

    @Data
    public static class Ssl {
        /** "SSL" (your cluster is SSL-only) */
        private String securityProtocol = "SSL";

        private String truststoreLocation;
        private String truststorePassword;
        private String truststoreType = "PKCS12";

        private String keystoreLocation;
        private String keystorePassword;
        private String keystoreType = "PKCS12";

        private String keyPassword;

        /**
         * Empty disables hostname verification (needed for IP-based brokers).
         * If you later move to DNS + SANs, set "HTTPS".
         */
        private String endpointIdentificationAlgorithm = "";
    }
}
