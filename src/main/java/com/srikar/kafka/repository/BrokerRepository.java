package com.srikar.kafka.repository;

import com.srikar.kafka.model.Broker;
import com.srikar.kafka.model.BrokerSnapshotRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface BrokerRepository extends JpaRepository<Broker, Long> {

    @Query(value = """
        SELECT
          server_name                                         AS server_name,
          node_id                                             AS node_id,
          role                                                AS role,
          (server_status = 1)                                 AS online,
          server_address                                      AS server_address,
          to_char(last_checked_at, 'YYYY-MM-DD HH24')
            || ':' || to_char(last_checked_at, 'MI')
            || ':' || to_char(last_checked_at, 'SS')
            || ' ' || to_char(last_checked_at, 'TZ')          AS last_checked_at,
          status_source                                       AS source
        FROM iaas_kafka.broker
        WHERE server_name = :pServerName
        ORDER BY node_id
        """, nativeQuery = true)
    List<BrokerSnapshotRow> snapshot(@Param("pServerName") String pServerName);


    @Modifying
    @Transactional
    @Query(value = """
        UPDATE iaas_kafka.broker
           SET server_status   = CASE WHEN :online THEN 1 ELSE 0 END,
               last_checked_at = now(),
               status_source   = :source,
               server_logs     = :logs
         WHERE server_name = :serverName
           AND node_id = :nodeId
        """, nativeQuery = true)
    void recordNodeStatus(@Param("serverName") String serverName,
                          @Param("nodeId") short nodeId,
                          @Param("online") boolean online,
                          @Param("source") String source,
                          @Param("logs") String logs);
}
