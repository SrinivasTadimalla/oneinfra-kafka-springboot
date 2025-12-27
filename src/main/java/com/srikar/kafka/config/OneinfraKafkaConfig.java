package com.srikar.kafka.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaAdminProperties.class)
public class OneinfraKafkaConfig {
    // Nothing else needed here.
    // This just registers KafkaAdminProperties as a Spring bean.
}
