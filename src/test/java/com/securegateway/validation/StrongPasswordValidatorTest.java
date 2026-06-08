package com.securegateway.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    static class Target {
        @StrongPassword
        String password;
        Target(String password) { this.password = password; }
    }

    @Test
    void accepts_strong_password() {
        assertThat(violations("Secure@123")).isEmpty();
    }

    @Test
    void rejects_missing_uppercase() {
        assertThat(violations("secure@123")).isNotEmpty();
    }

    @Test
    void rejects_missing_lowercase() {
        assertThat(violations("SECURE@123")).isNotEmpty();
    }

    @Test
    void rejects_missing_digit() {
        assertThat(violations("Secure@abc")).isNotEmpty();
    }

    @Test
    void rejects_missing_special_char() {
        assertThat(violations("Secure1234")).isNotEmpty();
    }

    @Test
    void allows_null() {
        assertThat(violations(null)).isEmpty();
    }

    private Set<ConstraintViolation<Target>> violations(String password) {
        return validator.validate(new Target(password));
    }
}
