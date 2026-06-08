package com.securegateway.repository;

import com.securegateway.model.SecurityEvent;
import com.securegateway.model.SecurityEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    List<SecurityEvent> findByTimestampAfterOrderByTimestampDesc(Instant since);

    List<SecurityEvent> findByEventTypeAndTimestampAfterOrderByTimestampDesc(
            SecurityEventType eventType, Instant since);

    long countByEventTypeAndTimestampAfter(SecurityEventType eventType, Instant since);
}
