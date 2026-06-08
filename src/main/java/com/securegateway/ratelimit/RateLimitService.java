package com.securegateway.ratelimit;

import com.securegateway.model.Role;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final RateLimitProperties props;

    public RateLimitService(StringRedisTemplate redis, RateLimitProperties props) {
        this.redis = redis;
        this.props = props;
    }

    public RateLimitResult checkAndIncrement(Long userId, Role role) {
        int limit = limitForRole(role);

        if (limit < 0) {
            return new RateLimitResult(true, -1, -1, 0);
        }

        long windowMinute = Instant.now().getEpochSecond() / 60;
        String key = "rl:" + userId + ":" + windowMinute;
        long resetEpoch = (windowMinute + 1) * 60;

        Long count = redis.opsForValue().increment(key);
        if (count == null) count = 1L;
        if (count == 1) {
            // 65s ensures the key outlives the window regardless of sub-second timing
            redis.expire(key, Duration.ofSeconds(65));
        }

        int remaining = (int) Math.max(0, limit - count);
        return new RateLimitResult(count <= limit, limit, remaining, resetEpoch);
    }

    private int limitForRole(Role role) {
        return switch (role) {
            case FREE -> props.getFreeRpm();
            case PRO -> props.getProRpm();
            case ADMIN -> props.getAdminRpm();
        };
    }
}
