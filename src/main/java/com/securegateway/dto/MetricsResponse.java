package com.securegateway.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class MetricsResponse {

    private final Instant generatedAt;
    private final int windowHours;
    private final LoginStats loginStats;
    private final long rateLimitRejections;
    private final long inputRejections;
    private final long tokenInvalidEvents;
    private final List<EndpointStat> topEndpoints;
    private final List<IpStat> topIps;
    private final Map<String, Long> eventsByType;

    public MetricsResponse(Instant generatedAt, int windowHours, LoginStats loginStats,
                           long rateLimitRejections, long inputRejections, long tokenInvalidEvents,
                           List<EndpointStat> topEndpoints, List<IpStat> topIps,
                           Map<String, Long> eventsByType) {
        this.generatedAt = generatedAt;
        this.windowHours = windowHours;
        this.loginStats = loginStats;
        this.rateLimitRejections = rateLimitRejections;
        this.inputRejections = inputRejections;
        this.tokenInvalidEvents = tokenInvalidEvents;
        this.topEndpoints = topEndpoints;
        this.topIps = topIps;
        this.eventsByType = eventsByType;
    }

    public Instant getGeneratedAt() { return generatedAt; }
    public int getWindowHours() { return windowHours; }
    public LoginStats getLoginStats() { return loginStats; }
    public long getRateLimitRejections() { return rateLimitRejections; }
    public long getInputRejections() { return inputRejections; }
    public long getTokenInvalidEvents() { return tokenInvalidEvents; }
    public List<EndpointStat> getTopEndpoints() { return topEndpoints; }
    public List<IpStat> getTopIps() { return topIps; }
    public Map<String, Long> getEventsByType() { return eventsByType; }

    public static class LoginStats {
        private final long successful;
        private final long failed;
        private final double failureRate;

        public LoginStats(long successful, long failed) {
            this.successful = successful;
            this.failed = failed;
            long total = successful + failed;
            this.failureRate = total > 0
                    ? Math.round((double) failed / total * 1000.0) / 1000.0
                    : 0.0;
        }

        public long getSuccessful() { return successful; }
        public long getFailed() { return failed; }
        public double getFailureRate() { return failureRate; }
    }

    public static class EndpointStat {
        private final String endpoint;
        private final long count;

        public EndpointStat(String endpoint, long count) {
            this.endpoint = endpoint;
            this.count = count;
        }

        public String getEndpoint() { return endpoint; }
        public long getCount() { return count; }
    }

    public static class IpStat {
        private final String ip;
        private final long count;

        public IpStat(String ip, long count) {
            this.ip = ip;
            this.count = count;
        }

        public String getIp() { return ip; }
        public long getCount() { return count; }
    }
}
