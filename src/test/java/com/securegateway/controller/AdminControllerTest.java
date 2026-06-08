package com.securegateway.controller;

import com.securegateway.dto.MetricsResponse;
import com.securegateway.model.SecurityEvent;
import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
import com.securegateway.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private SecurityEventRepository eventRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }

    @Test
    void returns_all_events_when_no_type_filter() throws Exception {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.LOGIN_FAILED)
                .ip("127.0.0.1")
                .userAgent("curl/7.0")
                .endpoint("POST /auth/login")
                .details("Invalid credentials")
                .userEmail("bad@example.com")
                .build();

        when(eventRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$[0].ip").value("127.0.0.1"))
                .andExpect(jsonPath("$[0].userEmail").value("bad@example.com"));
    }

    @Test
    void filters_events_by_type() throws Exception {
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.RATE_LIMIT_EXCEEDED)
                .ip("10.0.0.1")
                .userAgent("PostmanRuntime/7.0")
                .endpoint("GET /api/demo")
                .details("Limit: 10/min")
                .userEmail("free@example.com")
                .build();

        when(eventRepository.findByEventTypeAndTimestampAfterOrderByTimestampDesc(
                eq(SecurityEventType.RATE_LIMIT_EXCEEDED), any(Instant.class)))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/admin/events").param("type", "RATE_LIMIT_EXCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$[0].details").value("Limit: 10/min"));
    }

    @Test
    void returns_empty_list_when_no_events() throws Exception {
        when(eventRepository.findByTimestampAfterOrderByTimestampDesc(any(Instant.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/events").param("last", "1h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returns_metrics_with_default_24h_window() throws Exception {
        MetricsResponse metrics = new MetricsResponse(
                Instant.now(), 24,
                new MetricsResponse.LoginStats(42, 7),
                15L, 3L, 5L,
                List.of(new MetricsResponse.EndpointStat("GET /api/demo", 120)),
                List.of(new MetricsResponse.IpStat("192.168.1.1", 80)),
                Map.of("LOGIN_SUCCESS", 42L, "LOGIN_FAILED", 7L));

        when(metricsService.getMetrics(24)).thenReturn(metrics);

        mockMvc.perform(get("/admin/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowHours").value(24))
                .andExpect(jsonPath("$.loginStats.successful").value(42))
                .andExpect(jsonPath("$.loginStats.failed").value(7))
                .andExpect(jsonPath("$.rateLimitRejections").value(15))
                .andExpect(jsonPath("$.topEndpoints[0].endpoint").value("GET /api/demo"))
                .andExpect(jsonPath("$.topEndpoints[0].count").value(120))
                .andExpect(jsonPath("$.topIps[0].ip").value("192.168.1.1"));
    }

    @Test
    void metrics_respects_custom_window_hours() throws Exception {
        MetricsResponse metrics = new MetricsResponse(
                Instant.now(), 1,
                new MetricsResponse.LoginStats(5, 0),
                0L, 0L, 0L, List.of(), List.of(), Map.of());

        when(metricsService.getMetrics(1)).thenReturn(metrics);

        mockMvc.perform(get("/admin/metrics").param("windowHours", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowHours").value(1));
    }
}
