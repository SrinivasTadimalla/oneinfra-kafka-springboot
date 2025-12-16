package com.srikar.kafka;

import com.srikar.kafka.config.ProcessProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProcessProperties.class)
public class KafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
        System.out.println("Lord Balaji Kafka Spring Boot");
    }

}
