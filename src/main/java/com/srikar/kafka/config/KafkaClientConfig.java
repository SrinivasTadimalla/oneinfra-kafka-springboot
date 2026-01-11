package com.srikar.kafka.config;

import com.srikar.kafka.bootstrap.KafkaBootstrapResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaClientConfig {

    @Bean
    public KafkaClientPropertiesFactory kafkaClientPropertiesFactory(
            KafkaBootstrapResolver bootstrapResolver,
            KafkaAdminProperties kafkaAdminProperties
    ) {
        return new KafkaClientPropertiesFactory(bootstrapResolver, kafkaAdminProperties);
    }
}
