package com.coactivity.controller.impl;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SimpleTestController {

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        response.put("timestamp", java.time.Instant.now().toString());
        return response;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
