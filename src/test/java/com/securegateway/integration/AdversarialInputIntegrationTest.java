package com.securegateway.integration;

import com.securegateway.model.Role;
import com.securegateway.model.SecurityEventType;
import com.securegateway.model.User;
import com.securegateway.repository.SecurityEventRepository;
import com.securegateway.repository.UserRepository;
import com.securegateway.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adversarial tests: XSS, SQL injection, and rate limiting.
 * Rate limit is lowered to 3 req/min to keep the test fast.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "rate-limit.free-rpm=3")
class AdversarialInputIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired SecurityEventRepository securityEventRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        securityEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void xss_payload_in_data_field_returns_400() {
        String token = createFreeToken("xss@test.com");
        var body = Map.of("username", "alice", "message", "<script>alert('xss')</script>");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/data", HttpMethod.POST, authJsonBody(body, token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("Validation failed");
    }

    @Test
    void sql_injection_in_username_field_returns_400() {
        String token = createFreeToken("sqli@test.com");
        var body = Map.of("username", "' UNION SELECT * FROM users--", "message", "normal");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/data", HttpMethod.POST, authJsonBody(body, token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void input_rejection_logs_security_event() {
        String token = createFreeToken("input@test.com");
        var body = Map.of("username", "alice", "message", "<img src=x onerror=alert(1)>");

        restTemplate.exchange("/api/data", HttpMethod.POST, authJsonBody(body, token), Map.class);

        assertThat(securityEventRepository.findAll())
                .anyMatch(e -> e.getEventType() == SecurityEventType.INPUT_REJECTED);
    }

    @Test
    void free_user_is_rate_limited_after_3_requests() {
        String token = createFreeToken("ratelimit@test.com");

        for (int i = 0; i < 3; i++) {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    "/api/demo", HttpMethod.GET, authHeaders(token), Map.class);
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<Map> blocked = restTemplate.exchange(
                "/api/demo", HttpMethod.GET, authHeaders(token), Map.class);
        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void rate_limit_response_includes_ratelimit_headers() {
        String token = createFreeToken("headers@test.com");

        ResponseEntity<Map> first = restTemplate.exchange(
                "/api/demo", HttpMethod.GET, authHeaders(token), Map.class);

        assertThat(first.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("3");
        assertThat(first.getHeaders().getFirst("X-RateLimit-Remaining")).isNotNull();
        assertThat(first.getHeaders().getFirst("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    void rate_limit_exceeded_logs_security_event() {
        String token = createFreeToken("rlevt@test.com");

        for (int i = 0; i < 4; i++) {
            restTemplate.exchange("/api/demo", HttpMethod.GET, authHeaders(token), Map.class);
        }

        assertThat(securityEventRepository.findAll())
                .anyMatch(e -> e.getEventType() == SecurityEventType.RATE_LIMIT_EXCEEDED);
    }

    private String createFreeToken(String email) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Secure@123"))
                .role(Role.FREE)
                .build());
        return jwtService.generateToken(user);
    }
}
