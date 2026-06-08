package com.securegateway.controller;

import com.securegateway.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
