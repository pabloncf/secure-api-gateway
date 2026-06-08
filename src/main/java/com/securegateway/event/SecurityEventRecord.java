package com.securegateway.event;

import com.securegateway.model.SecurityEventType;

public record SecurityEventRecord(
        SecurityEventType type,
        String ip,
        String userAgent,
        String endpoint,
        String details,
        String userEmail
) {}
