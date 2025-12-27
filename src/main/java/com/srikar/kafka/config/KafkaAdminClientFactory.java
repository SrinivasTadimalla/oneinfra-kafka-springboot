package com.srikar.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaAdminClientFactory {

    private final KafkaAdminProperties props;

    public AdminClient create(String bootstrapServers) {

        String resolvedBootstrap = resolveBootstrap(bootstrapServers);

        Map<String, Object> cfg = new HashMap<>();

        // --- core ---
        cfg.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, resolvedBootstrap);
        cfg.put(CommonClientConfigs.CLIENT_ID_CONFIG, props.getClientId());

        // --- timeouts (TLS realistic) ---
        cfg.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, props.getRequestTimeoutMs());
        cfg.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, props.getDefaultApiTimeoutMs());

        // --- TLS handshake / initial connect helpers ---
        cfg.put(CommonClientConfigs.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG, props.getSocketSetupTimeoutMs());
        cfg.put(CommonClientConfigs.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG, props.getSocketSetupTimeoutMaxMs());

        // --- SSL / mTLS ---
        KafkaAdminProperties.Ssl ssl = props.getSsl();

        cfg.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, ssl.getSecurityProtocol()); // "SSL"

        cfg.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ssl.getTruststoreLocation());
        cfg.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.getTruststorePassword());
        cfg.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, ssl.getTruststoreType());

        cfg.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ssl.getKeystoreLocation());
        cfg.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ssl.getKeystorePassword());
        cfg.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, ssl.getKeystoreType());
        cfg.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, ssl.getKeyPassword());

        // IP-based brokers => keep empty unless you have DNS SANs
        cfg.put(
                SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,
                ssl.getEndpointIdentificationAlgorithm() == null ? "" : ssl.getEndpointIdentificationAlgorithm()
        );

        return AdminClient.create(cfg);
    }

    private String resolveBootstrap(String bootstrapServers) {
        if (bootstrapServers != null && !bootstrapServers.isBlank()) {
            return bootstrapServers;
        }

        List<String> defaults = props.getBootstrapServers();
        if (defaults == null || defaults.isEmpty()) {
            throw new IllegalStateException(
                    "No bootstrapServers provided and oneinfra.kafka.admin.bootstrap-servers is empty"
            );
        }
        return String.join(",", defaults);
    }
}
