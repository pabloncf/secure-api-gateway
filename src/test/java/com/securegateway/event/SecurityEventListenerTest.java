package com.securegateway.event;

import com.securegateway.model.SecurityEvent;
import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityEventListenerTest {

    @Mock
    private SecurityEventRepository repository;

    @InjectMocks
    private SecurityEventListener listener;

    @Test
    void persists_login_failed_event() {
        SecurityEventRecord record = new SecurityEventRecord(
                SecurityEventType.LOGIN_FAILED, "127.0.0.1",
                "Mozilla/5.0", "POST /auth/login",
                "Invalid credentials", "user@example.com");

        listener.onSecurityEvent(record);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(repository).save(captor.capture());

        SecurityEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(SecurityEventType.LOGIN_FAILED);
        assertThat(saved.getIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getUserEmail()).isEqualTo("user@example.com");
        assertThat(saved.getDetails()).isEqualTo("Invalid credentials");
        assertThat(saved.getEndpoint()).isEqualTo("POST /auth/login");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void persists_token_invalid_event_without_user_email() {
        SecurityEventRecord record = new SecurityEventRecord(
                SecurityEventType.TOKEN_INVALID, "10.0.0.1",
                "curl/7.0", "GET /api/demo",
                "Token expired", null);

        listener.onSecurityEvent(record);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(repository).save(captor.capture());

        SecurityEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(SecurityEventType.TOKEN_INVALID);
        assertThat(saved.getUserEmail()).isNull();
        assertThat(saved.getDetails()).isEqualTo("Token expired");
    }

    @Test
    void persists_rate_limit_exceeded_event() {
        SecurityEventRecord record = new SecurityEventRecord(
                SecurityEventType.RATE_LIMIT_EXCEEDED, "192.168.1.1",
                "PostmanRuntime/7.0", "GET /api/demo",
                "Limit: 10/min", "free@example.com");

        listener.onSecurityEvent(record);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getEventType()).isEqualTo(SecurityEventType.RATE_LIMIT_EXCEEDED);
    }
}
