package com.securegateway.ratelimit;

public record RateLimitResult(boolean allowed, int limit, int remaining, long resetEpochSecond) {}
