package com.srikar.kafka.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "broker", schema = "iaas_kafka")
@Getter
@Setter
@NoArgsConstructor
public class Broker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_name")
    private String serverName;

    @Column(name = "server_address")
    private String serverAddress;

    @Column(name = "description")
    private String description;

    @Column(name = "meta_data")
    private String metaData;

    @Column(name = "server_status")
    private Short serverStatus;  // 0/1/2

    @Column(name = "server_logs")
    private String serverLogs;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "broker_id")
    private Short brokerId;
}
