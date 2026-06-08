package com.securegateway.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SafeStringValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    static class Target {
        @SafeString
        String value;
        Target(String value) { this.value = value; }
    }

    @Test
    void accepts_normal_text() {
        assertThat(violations("Hello, this is a normal message!")).isEmpty();
    }

    @Test
    void accepts_text_with_numbers_and_punctuation() {
        assertThat(violations("Order #123 placed on 2024-01-01.")).isEmpty();
    }

    @Test
    void rejects_script_tag() {
        assertThat(violations("<script>alert('xss')</script>")).isNotEmpty();
    }

    @Test
    void rejects_html_tag() {
        assertThat(violations("<img src=x>")).isNotEmpty();
    }

    @Test
    void rejects_sql_union_select() {
        assertThat(violations("' UNION SELECT * FROM users--")).isNotEmpty();
    }

    @Test
    void rejects_sql_drop_table() {
        assertThat(violations("; DROP TABLE users")).isNotEmpty();
    }

    @Test
    void rejects_javascript_protocol() {
        assertThat(violations("javascript:alert(1)")).isNotEmpty();
    }

    @Test
    void rejects_xss_event_handler() {
        assertThat(violations("test onerror=alert(1)")).isNotEmpty();
    }

    @Test
    void allows_null() {
        assertThat(violations(null)).isEmpty();
    }

    private Set<ConstraintViolation<Target>> violations(String value) {
        return validator.validate(new Target(value));
    }
}
