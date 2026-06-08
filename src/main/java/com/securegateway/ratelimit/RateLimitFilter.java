package com.securegateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securegateway.event.SecurityEventPublisher;
import com.securegateway.model.SecurityEventType;
import com.securegateway.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final SecurityEventPublisher eventPublisher;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper,
                           SecurityEventPublisher eventPublisher) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimitService.checkAndIncrement(user.getId(), user.getRole());

        if (result.limit() > 0) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetEpochSecond()));
        }

        if (!result.allowed()) {
            eventPublisher.publish(SecurityEventType.RATE_LIMIT_EXCEEDED, request,
                    "Limit: " + result.limit() + "/min", user.getEmail());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status", 429,
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Resets at epoch second " + result.resetEpochSecond()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
