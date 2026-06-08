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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccessControlIntegrationTest extends AbstractIntegrationTest {

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
    void unauthenticated_request_returns_401() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/demo", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tampered_token_returns_401_and_logs_event() {
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXJAZXhhbXBsZS5jb20ifQ.BADSIGNATURE";

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/demo", HttpMethod.GET, authHeaders(fakeToken), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(securityEventRepository.findAll())
                .anyMatch(e -> e.getEventType() == SecurityEventType.TOKEN_INVALID);
    }

    @Test
    void valid_free_token_grants_access_to_api() {
        String token = createUserAndToken("free@test.com", Role.FREE);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/demo", HttpMethod.GET, authHeaders(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("email")).isEqualTo("free@test.com");
        assertThat(response.getBody().get("role")).isEqualTo("FREE");
    }

    @Test
    void free_token_cannot_access_admin_events_returns_403() {
        String token = createUserAndToken("free@test.com", Role.FREE);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/events", HttpMethod.GET, authHeaders(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void admin_token_can_access_events_endpoint() {
        String token = createUserAndToken("admin@test.com", Role.ADMIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/events", HttpMethod.GET, authHeaders(token), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void admin_token_can_access_metrics_endpoint() {
        String token = createUserAndToken("admin@test.com", Role.ADMIN);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/metrics", HttpMethod.GET, authHeaders(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("loginStats");
        assertThat(response.getBody()).containsKey("topEndpoints");
        assertThat(response.getBody()).containsKey("eventsByType");
    }

    @Test
    void security_headers_present_on_every_response() {
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeaders().getFirst("X-XSS-Protection")).isEqualTo("1; mode=block");
        assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
    }

    private String createUserAndToken(String email, Role role) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode("Secure@123"))
                .role(role)
                .build());
        return jwtService.generateToken(user);
    }
}
