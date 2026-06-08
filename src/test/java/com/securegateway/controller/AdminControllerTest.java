package com.securegateway.controller;

import com.securegateway.model.SecurityEvent;
import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
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
}
