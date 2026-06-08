package com.securegateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securegateway.dto.DataRequest;
import com.securegateway.event.SecurityEventPublisher;
import com.securegateway.exception.GlobalExceptionHandler;
import com.securegateway.model.Role;
import com.securegateway.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiControllerValidationTest {

    @Mock
    private SecurityEventPublisher eventPublisher;

    @InjectMocks
    private ApiController apiController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User authenticatedUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(apiController)
                .setControllerAdvice(new GlobalExceptionHandler(eventPublisher))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        authenticatedUser = User.builder()
                .email("user@example.com")
                .role(Role.FREE)
                .password("hashed")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitData_returns200_for_valid_input() throws Exception {
        DataRequest request = new DataRequest();
        request.setUsername("alice");
        request.setMessage("Hello, world!");

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.submittedBy").value("user@example.com"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void submitData_returns400_for_xss_in_message() throws Exception {
        DataRequest request = new DataRequest();
        request.setUsername("alice");
        request.setMessage("<script>alert('xss')</script>");

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.message").exists());
    }

    @Test
    void submitData_returns400_for_sql_injection_in_username() throws Exception {
        DataRequest request = new DataRequest();
        request.setUsername("' UNION SELECT * FROM users--");
        request.setMessage("Normal message");

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void submitData_returns400_when_message_is_blank() throws Exception {
        DataRequest request = new DataRequest();
        request.setUsername("alice");
        request.setMessage("");

        mockMvc.perform(post("/api/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.message").exists());
    }
}
