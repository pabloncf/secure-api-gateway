package com.securegateway.service;

import com.securegateway.dto.MetricsResponse;
import com.securegateway.dto.MetricsResponse.EndpointStat;
import com.securegateway.dto.MetricsResponse.IpStat;
import com.securegateway.dto.MetricsResponse.LoginStats;
import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private static final int MAX_WINDOW_HOURS = 168; // 7 days

    private final SecurityEventRepository eventRepository;

    public MetricsService(SecurityEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public MetricsResponse getMetrics(int windowHours) {
        int capped = Math.min(Math.max(windowHours, 1), MAX_WINDOW_HOURS);
        Instant since = Instant.now().minus(capped, ChronoUnit.HOURS);

        Map<String, Long> eventsByType = buildEventsByType(since);

        long loginSuccess = eventsByType.getOrDefault(SecurityEventType.LOGIN_SUCCESS.name(), 0L);
        long loginFailed = eventsByType.getOrDefault(SecurityEventType.LOGIN_FAILED.name(), 0L);
        long rateLimitRejections = eventsByType.getOrDefault(SecurityEventType.RATE_LIMIT_EXCEEDED.name(), 0L);
        long inputRejections = eventsByType.getOrDefault(SecurityEventType.INPUT_REJECTED.name(), 0L);
        long tokenInvalid = eventsByType.getOrDefault(SecurityEventType.TOKEN_INVALID.name(), 0L);

        List<EndpointStat> topEndpoints = eventRepository.topEndpoints(since).stream()
                .map(row -> new EndpointStat((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<IpStat> topIps = eventRepository.topIps(since).stream()
                .map(row -> new IpStat((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        return new MetricsResponse(
                Instant.now(), capped,
                new LoginStats(loginSuccess, loginFailed),
                rateLimitRejections, inputRejections, tokenInvalid,
                topEndpoints, topIps, eventsByType);
    }

    private Map<String, Long> buildEventsByType(Instant since) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (SecurityEventType type : SecurityEventType.values()) {
            result.put(type.name(), 0L);
        }
        for (Object[] row : eventRepository.countGroupByEventType(since)) {
            SecurityEventType type = (SecurityEventType) row[0];
            result.put(type.name(), (Long) row[1]);
        }
        return result;
    }
}
