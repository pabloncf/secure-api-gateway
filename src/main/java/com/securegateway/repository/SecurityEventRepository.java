package com.securegateway.repository;

import com.securegateway.model.SecurityEvent;
import com.securegateway.model.SecurityEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByTimestampAfterOrderByTimestampDesc(Instant since);

    List<SecurityEvent> findByEventTypeAndTimestampAfterOrderByTimestampDesc(
            SecurityEventType eventType, Instant since);

    long countByEventTypeAndTimestampAfter(SecurityEventType eventType, Instant since);

    @Query("SELECT e.eventType, COUNT(e) FROM SecurityEvent e WHERE e.timestamp >= :since GROUP BY e.eventType")
    List<Object[]> countGroupByEventType(@Param("since") Instant since);

    @Query(value = "SELECT endpoint, COUNT(*) AS cnt FROM security_events WHERE timestamp >= :since GROUP BY endpoint ORDER BY cnt DESC LIMIT 5",
            nativeQuery = true)
    List<Object[]> topEndpoints(@Param("since") Instant since);

    @Query(value = "SELECT ip, COUNT(*) AS cnt FROM security_events WHERE timestamp >= :since AND ip IS NOT NULL GROUP BY ip ORDER BY cnt DESC LIMIT 5",
            nativeQuery = true)
    List<Object[]> topIps(@Param("since") Instant since);
}
