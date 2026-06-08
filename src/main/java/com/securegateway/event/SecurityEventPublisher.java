package com.securegateway.event;

import com.securegateway.model.SecurityEventType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SecurityEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(SecurityEventType type, HttpServletRequest request,
                        String details, String userEmail) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        publisher.publishEvent(new SecurityEventRecord(type, ip, userAgent, endpoint, details, userEmail));
    }
}
