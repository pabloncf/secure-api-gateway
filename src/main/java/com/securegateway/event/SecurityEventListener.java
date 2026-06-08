package com.securegateway.event;

import com.securegateway.model.SecurityEvent;
import com.securegateway.repository.SecurityEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecurityEventListener {

    private final SecurityEventRepository repository;

    public SecurityEventListener(SecurityEventRepository repository) {
        this.repository = repository;
    }

    @EventListener
    @Transactional
    public void onSecurityEvent(SecurityEventRecord record) {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(record.type())
                .ip(record.ip())
                .userAgent(record.userAgent())
                .endpoint(record.endpoint())
                .details(record.details())
                .userEmail(record.userEmail())
                .build();
        repository.save(event);
    }
}
