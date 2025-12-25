package com.srikar.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "oneinfra.kafka.admin.ssl")
public class KafkaSslProperties {
    private String securityProtocol = "SSL";

    private String truststoreLocation;
    private String truststorePassword;
    private String truststoreType = "PKCS12";

    private String keystoreLocation;
    private String keystorePassword;
    private String keystoreType = "PKCS12";

    private String keyPassword;

    /** empty disables hostname verification (IP based clusters) */
    private String endpointIdentificationAlgorithm = "";
}
