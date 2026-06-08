package com.securegateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securegateway.event.SecurityEventPublisher;
import com.securegateway.model.Role;
import com.securegateway.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SecurityEventPublisher eventPublisher;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesThroughWithoutAuth() throws Exception {
        SecurityContextHolder.clearContext();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(rateLimitService);
    }

    @Test
    void allowsRequestAndSetsRateLimitHeaders() throws Exception {
        authenticateUser(Role.FREE);
        when(rateLimitService.checkAndIncrement(anyLong(), any()))
                .thenReturn(new RateLimitResult(true, 10, 8, 99999L));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-RateLimit-Limit", "10");
        verify(response).setHeader("X-RateLimit-Remaining", "8");
        verify(response).setHeader("X-RateLimit-Reset", "99999");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void returns429WhenLimitExceeded() throws Exception {
        authenticateUser(Role.FREE);
        when(rateLimitService.checkAndIncrement(anyLong(), any()))
                .thenReturn(new RateLimitResult(false, 10, 0, 99999L));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void adminRequestSkipsRateLimitHeaders() throws Exception {
        authenticateUser(Role.ADMIN);
        when(rateLimitService.checkAndIncrement(anyLong(), any()))
                .thenReturn(new RateLimitResult(true, -1, -1, 0));

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response, never()).setHeader(eq("X-RateLimit-Limit"), any());
        verify(filterChain).doFilter(request, response);
    }

    private void authenticateUser(Role role) {
        User user = User.builder().id(1L).email("user@test.com").password("x").role(role).build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
