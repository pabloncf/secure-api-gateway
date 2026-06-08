package com.securegateway.ratelimit;

import com.securegateway.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private RateLimitProperties props;

    @InjectMocks
    private RateLimitService rateLimitService;

    @Test
    void adminIsAlwaysAllowedWithoutRedis() {
        when(props.getAdminRpm()).thenReturn(-1);

        RateLimitResult result = rateLimitService.checkAndIncrement(1L, Role.ADMIN);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(-1);
        verifyNoInteractions(redis);
    }

    @Test
    void freeUserIsAllowedUnderLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(props.getFreeRpm()).thenReturn(10);
        when(valueOps.increment(anyString())).thenReturn(5L);

        RateLimitResult result = rateLimitService.checkAndIncrement(1L, Role.FREE);

        assertThat(result.allowed()).isTrue();
        assertThat(result.remaining()).isEqualTo(5);
        assertThat(result.limit()).isEqualTo(10);
    }

    @Test
    void freeUserIsBlockedWhenLimitExceeded() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(props.getFreeRpm()).thenReturn(10);
        when(valueOps.increment(anyString())).thenReturn(11L);

        RateLimitResult result = rateLimitService.checkAndIncrement(1L, Role.FREE);

        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isEqualTo(0);
    }

    @Test
    void proUserHasHigherLimit() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(props.getProRpm()).thenReturn(60);
        when(valueOps.increment(anyString())).thenReturn(30L);

        RateLimitResult result = rateLimitService.checkAndIncrement(2L, Role.PRO);

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(60);
        assertThat(result.remaining()).isEqualTo(30);
    }

    @Test
    void firstRequestSetsKeyExpiry() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(props.getFreeRpm()).thenReturn(10);
        when(valueOps.increment(anyString())).thenReturn(1L);

        rateLimitService.checkAndIncrement(1L, Role.FREE);

        verify(redis).expire(anyString(), any());
    }

    @Test
    void subsequentRequestsDoNotResetExpiry() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(props.getFreeRpm()).thenReturn(10);
        when(valueOps.increment(anyString())).thenReturn(2L);

        rateLimitService.checkAndIncrement(1L, Role.FREE);

        verify(redis, never()).expire(anyString(), any());
    }
}
