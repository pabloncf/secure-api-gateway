package com.securegateway.dto;

import com.securegateway.validation.SafeString;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DataRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @SafeString
    private String username;

    @NotBlank
    @Size(max = 500)
    @SafeString
    private String message;

    @Size(max = 2000)
    @SafeString
    private String notes;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
