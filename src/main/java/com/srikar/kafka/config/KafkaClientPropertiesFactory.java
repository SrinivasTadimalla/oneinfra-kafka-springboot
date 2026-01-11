package com.srikar.kafka.config;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

import java.util.Properties;
import java.util.UUID;

public class KafkaClientPropertiesFactory {

    private final KafkaBootstrapResolver bootstrapResolver;
    private final KafkaAdminProperties props;

    public KafkaClientPropertiesFactory(KafkaBootstrapResolver bootstrapResolver,
                                        KafkaAdminProperties props) {
        this.bootstrapResolver = bootstrapResolver;
        this.props = props;
    }

    public Properties base(String clusterName) {

        String bootstrap = bootstrapResolver.resolve(clusterName);

        Properties p = new Properties();

        // Core
        p.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(CommonClientConfigs.CLIENT_ID_CONFIG, "oneinfra-" + UUID.randomUUID());

        // SSL / mTLS
        KafkaAdminProperties.Ssl ssl = props.getSsl();
        if (ssl == null) {
            throw new IllegalStateException("Kafka SSL settings missing: props.getSsl() == null");
        }

        p.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, ssl.getSecurityProtocol()); // "SSL"

        p.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ssl.getTruststoreLocation());
        p.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ssl.getTruststorePassword());
        p.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, ssl.getTruststoreType());

        p.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ssl.getKeystoreLocation());
        p.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ssl.getKeystorePassword());
        p.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, ssl.getKeystoreType());
        p.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, ssl.getKeyPassword());

        // If using IP-based broker SANs and you don’t want hostname verification
        p.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        return p;
    }
}
