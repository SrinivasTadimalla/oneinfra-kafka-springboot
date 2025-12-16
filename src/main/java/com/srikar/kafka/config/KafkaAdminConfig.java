package com.srikar.kafka.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class KafkaAdminConfig {

    private AdminClient adminClient;

    // ----- From application.yml → spring.kafka.* -----

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ✅ read from YAML instead of hardcoding "SSL"
    @Value("${spring.kafka.properties.security.protocol:SSL}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.ssl.truststore.location}")
    private String truststoreLocation;

    @Value("${spring.kafka.properties.ssl.truststore.password}")
    private String truststorePassword;

    @Value("${spring.kafka.properties.ssl.truststore.type:PKCS12}")
    private String truststoreType;

    @Value("${spring.kafka.properties.ssl.keystore.location}")
    private String keystoreLocation;

    @Value("${spring.kafka.properties.ssl.keystore.password}")
    private String keystorePassword;

    @Value("${spring.kafka.properties.ssl.keystore.type:PKCS12}")
    private String keystoreType;

    @Value("${spring.kafka.properties.ssl.key.password}")
    private String keyPassword;

    @Value("${spring.kafka.properties.ssl.endpoint.identification.algorithm:}")
    private String endpointIdentificationAlgorithm;

    @Bean
    public AdminClient kafkaAdminClient() {

        log.info("Kafka AdminClient configuration started (securityProtocol={})", securityProtocol);

        Map<String, Object> config = new HashMap<>();

        // ---- Broker endpoints (SSL listeners on 9093) ----
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);

        // ---- Truststore (trusts broker cert / CA) ----
        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
        config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType);

        // ---- Keystore (client identity for mTLS) ----
        config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystoreLocation);
        config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePassword);
        config.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, keystoreType);
        config.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPassword);

        // ---- Hostname verification ----
        // "" disables verification (useful for IPs). Prefer DNS + keep this enabled in prod.
        config.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentificationAlgorithm);

        log.info("Kafka AdminClient (SSL/mTLS) bootstrap.servers={}", bootstrapServers);
        log.info("truststore={}", truststoreLocation);
        log.info("keystore={}", keystoreLocation);

        this.adminClient = AdminClient.create(config);
        return this.adminClient;
    }

    @PreDestroy
    public void closeAdminClient() {
        if (adminClient != null) {
            log.info("Closing Kafka AdminClient");
            adminClient.close();
        }
    }
}
