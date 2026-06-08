package com.securegateway.integration;

import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
import com.securegateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired UserRepository userRepository;
    @Autowired SecurityEventRepository securityEventRepository;

    @BeforeEach
    void cleanDatabase() {
        securityEventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void register_creates_free_user_and_returns_token() {
        var body = Map.of("email", "new@test.com", "password", "Secure@123");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/auth/register", HttpMethod.POST, jsonBody(body), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("token");
        assertThat(response.getBody().get("role")).isEqualTo("FREE");
    }

    @Test
    void register_duplicate_email_returns_409() {
        var body = Map.of("email", "dup@test.com", "password", "Secure@123");
        restTemplate.exchange("/auth/register", HttpMethod.POST, jsonBody(body), Map.class);

        ResponseEntity<Map> second = restTemplate.exchange(
                "/auth/register", HttpMethod.POST, jsonBody(body), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_invalid_email_returns_400() {
        var body = Map.of("email", "not-an-email", "password", "Secure@123");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/auth/register", HttpMethod.POST, jsonBody(body), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("fieldErrors");
    }

    @Test
    void register_weak_password_returns_400() {
        var body = Map.of("email", "user@test.com", "password", "weakpass");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/auth/register", HttpMethod.POST, jsonBody(body), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_returns_token_on_valid_credentials() {
        var reg = Map.of("email", "user@test.com", "password", "Secure@123");
        restTemplate.exchange("/auth/register", HttpMethod.POST, jsonBody(reg), Map.class);

        var login = Map.of("email", "user@test.com", "password", "Secure@123");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/auth/login", HttpMethod.POST, jsonBody(login), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("token");
        assertThat((String) response.getBody().get("token")).isNotBlank();
    }

    @Test
    void login_wrong_password_returns_401() {
        var reg = Map.of("email", "user@test.com", "password", "Secure@123");
        restTemplate.exchange("/auth/register", HttpMethod.POST, jsonBody(reg), Map.class);

        var login = Map.of("email", "user@test.com", "password", "WrongPass@9");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/auth/login", HttpMethod.POST, jsonBody(login), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_failure_persists_login_failed_security_event() {
        var reg = Map.of("email", "user@test.com", "password", "Secure@123");
        restTemplate.exchange("/auth/register", HttpMethod.POST, jsonBody(reg), Map.class);

        var login = Map.of("email", "user@test.com", "password", "BadPass@9");
        restTemplate.exchange("/auth/login", HttpMethod.POST, jsonBody(login), Map.class);

        var events = securityEventRepository.findAll();
        assertThat(events).anyMatch(e ->
                e.getEventType() == SecurityEventType.LOGIN_FAILED
                        && "user@test.com".equals(e.getUserEmail()));
    }
}
