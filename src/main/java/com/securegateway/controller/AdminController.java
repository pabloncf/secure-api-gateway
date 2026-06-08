package com.securegateway.controller;

import com.securegateway.dto.SecurityEventResponse;
import com.securegateway.model.SecurityEventType;
import com.securegateway.repository.SecurityEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final SecurityEventRepository eventRepository;

    public AdminController(SecurityEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/events")
    public ResponseEntity<List<SecurityEventResponse>> getEvents(
            @RequestParam(required = false) SecurityEventType type,
            @RequestParam(required = false, defaultValue = "24h") String last) {

        Instant since = parseLast(last);
        List<SecurityEventResponse> events = (type != null
                ? eventRepository.findByEventTypeAndTimestampAfterOrderByTimestampDesc(type, since)
                : eventRepository.findByTimestampAfterOrderByTimestampDesc(since))
                .stream()
                .map(e -> new SecurityEventResponse(
                        e.getId(), e.getTimestamp(), e.getIp(), e.getUserAgent(),
                        e.getEndpoint(), e.getEventType(), e.getDetails(), e.getUserEmail()))
                .toList();

        return ResponseEntity.ok(events);
    }

    private Instant parseLast(String last) {
        if (last == null || last.isBlank()) return Instant.now().minus(24, ChronoUnit.HOURS);
        try {
            if (last.endsWith("h")) {
                return Instant.now().minus(Long.parseLong(last.replace("h", "")), ChronoUnit.HOURS);
            }
            if (last.endsWith("d")) {
                return Instant.now().minus(Long.parseLong(last.replace("d", "")), ChronoUnit.DAYS);
            }
        } catch (NumberFormatException ignored) {}
        return Instant.now().minus(24, ChronoUnit.HOURS);
    }
}
