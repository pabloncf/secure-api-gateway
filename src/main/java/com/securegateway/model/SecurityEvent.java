package com.securegateway.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_sec_events_type_ts", columnList = "event_type, timestamp"),
        @Index(name = "idx_sec_events_ts", columnList = "timestamp")
})
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(length = 45)
    private String ip;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private SecurityEventType eventType;

    @Column(length = 1000)
    private String details;

    @Column(length = 255)
    private String userEmail;

    public SecurityEvent() {}

    private SecurityEvent(Builder b) {
        this.timestamp = b.timestamp;
        this.ip = b.ip;
        this.userAgent = b.userAgent;
        this.endpoint = b.endpoint;
        this.eventType = b.eventType;
        this.details = b.details;
        this.userEmail = b.userEmail;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public String getEndpoint() { return endpoint; }
    public SecurityEventType getEventType() { return eventType; }
    public String getDetails() { return details; }
    public String getUserEmail() { return userEmail; }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private String ip;
        private String userAgent;
        private String endpoint;
        private SecurityEventType eventType;
        private String details;
        private String userEmail;

        public Builder ip(String ip) { this.ip = ip; return this; }
        public Builder userAgent(String userAgent) { this.userAgent = userAgent; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder eventType(SecurityEventType type) { this.eventType = type; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder userEmail(String userEmail) { this.userEmail = userEmail; return this; }
        public SecurityEvent build() { return new SecurityEvent(this); }
    }
}
