package com.securegateway.controller;

import com.securegateway.dto.DataRequest;
import com.securegateway.model.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/demo")
    public ResponseEntity<Map<String, Object>> demo(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "message", "Access granted",
                "email", user.getEmail(),
                "role", user.getRole().name()
        ));
    }

    @PostMapping("/data")
    public ResponseEntity<Map<String, Object>> submitData(
            @Valid @RequestBody DataRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "processed", true,
                "submittedBy", user.getEmail(),
                "username", request.getUsername(),
                "message", request.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
