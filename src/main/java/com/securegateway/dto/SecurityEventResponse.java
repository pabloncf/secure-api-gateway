package com.securegateway.dto;

import com.securegateway.model.SecurityEventType;

import java.time.Instant;

public class SecurityEventResponse {

    private final Long id;
    private final Instant timestamp;
    private final String ip;
    private final String userAgent;
    private final String endpoint;
    private final SecurityEventType eventType;
    private final String details;
    private final String userEmail;

    public SecurityEventResponse(Long id, Instant timestamp, String ip, String userAgent,
                                 String endpoint, SecurityEventType eventType,
                                 String details, String userEmail) {
        this.id = id;
        this.timestamp = timestamp;
        this.ip = ip;
        this.userAgent = userAgent;
        this.endpoint = endpoint;
        this.eventType = eventType;
        this.details = details;
        this.userEmail = userEmail;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public String getEndpoint() { return endpoint; }
    public SecurityEventType getEventType() { return eventType; }
    public String getDetails() { return details; }
    public String getUserEmail() { return userEmail; }
}
