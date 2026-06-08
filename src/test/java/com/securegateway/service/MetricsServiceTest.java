package com.securegateway.service;

import com.securegateway.dto.MetricsResponse;
import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private SecurityEventRepository eventRepository;

    @InjectMocks
    private MetricsService metricsService;

    @Test
    void returns_correct_counts_from_event_type_aggregation() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of(
                new Object[]{SecurityEventType.LOGIN_SUCCESS, 10L},
                new Object[]{SecurityEventType.LOGIN_FAILED, 3L},
                new Object[]{SecurityEventType.RATE_LIMIT_EXCEEDED, 5L}
        ));
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of());

        MetricsResponse result = metricsService.getMetrics(24);

        assertThat(result.getLoginStats().getSuccessful()).isEqualTo(10);
        assertThat(result.getLoginStats().getFailed()).isEqualTo(3);
        assertThat(result.getRateLimitRejections()).isEqualTo(5);
        assertThat(result.getInputRejections()).isZero();
        assertThat(result.getTokenInvalidEvents()).isZero();
    }

    @Test
    void computes_failure_rate_correctly() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of(
                new Object[]{SecurityEventType.LOGIN_SUCCESS, 7L},
                new Object[]{SecurityEventType.LOGIN_FAILED, 3L}
        ));
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of());

        MetricsResponse result = metricsService.getMetrics(24);

        // 3 / (7 + 3) = 0.3
        assertThat(result.getLoginStats().getFailureRate()).isEqualTo(0.3);
    }

    @Test
    void returns_zero_failure_rate_when_no_login_events() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of());

        MetricsResponse result = metricsService.getMetrics(24);

        assertThat(result.getLoginStats().getFailureRate()).isZero();
        assertThat(result.getLoginStats().getSuccessful()).isZero();
        assertThat(result.getLoginStats().getFailed()).isZero();
    }

    @Test
    void maps_top_endpoints_correctly() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of(
                new Object[]{"GET /api/demo", 120L},
                new Object[]{"POST /auth/login", 45L}
        ));
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of());

        MetricsResponse result = metricsService.getMetrics(24);

        assertThat(result.getTopEndpoints()).hasSize(2);
        assertThat(result.getTopEndpoints().get(0).getEndpoint()).isEqualTo("GET /api/demo");
        assertThat(result.getTopEndpoints().get(0).getCount()).isEqualTo(120);
        assertThat(result.getTopEndpoints().get(1).getEndpoint()).isEqualTo("POST /auth/login");
    }

    @Test
    void maps_top_ips_correctly() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of(
                new Object[]{"192.168.1.1", 80L},
                new Object[]{"10.0.0.1", 30L}
        ));

        MetricsResponse result = metricsService.getMetrics(24);

        assertThat(result.getTopIps()).hasSize(2);
        assertThat(result.getTopIps().get(0).getIp()).isEqualTo("192.168.1.1");
        assertThat(result.getTopIps().get(0).getCount()).isEqualTo(80);
    }

    @Test
    void caps_window_hours_at_168() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of());

        MetricsResponse result = metricsService.getMetrics(9999);

        assertThat(result.getWindowHours()).isEqualTo(168);
    }

    @Test
    void eventsByType_contains_all_types_with_zero_defaults() {
        when(eventRepository.countGroupByEventType(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topEndpoints(any(Instant.class))).thenReturn(List.of());
        when(eventRepository.topIps(any(Instant.class))).thenReturn(List.of());

        MetricsResponse result = metricsService.getMetrics(24);

        assertThat(result.getEventsByType()).containsKey("LOGIN_SUCCESS");
        assertThat(result.getEventsByType()).containsKey("LOGIN_FAILED");
        assertThat(result.getEventsByType()).containsKey("RATE_LIMIT_EXCEEDED");
        assertThat(result.getEventsByType()).containsKey("INPUT_REJECTED");
        assertThat(result.getEventsByType()).containsKey("TOKEN_INVALID");
        assertThat(result.getEventsByType().values()).allMatch(v -> v == 0L);
    }
}
