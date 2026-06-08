package com.securegateway.dto;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {

    private final int status;
    private final String error;
    private final Map<String, String> fieldErrors;
    private final Instant timestamp;

    public ErrorResponse(int status, String error, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.fieldErrors = fieldErrors;
        this.timestamp = Instant.now();
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public Instant getTimestamp() { return timestamp; }
}
