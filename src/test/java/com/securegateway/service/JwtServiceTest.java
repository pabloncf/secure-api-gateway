package com.securegateway.service;

import com.securegateway.model.Role;
import com.securegateway.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm";
        jwtService = new JwtService(secret, 3_600_000L);
    }

    @Test
    void generatedTokenIsValid() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void extractEmailReturnsCorrectSubject() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService shortLived = new JwtService(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm", -1L);
        String token = shortLived.generateToken(buildUser());
        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    void tamperedTokenIsInvalid() {
        User user = buildUser();
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("hashed")
                .role(Role.FREE)
                .build();
    }
}
