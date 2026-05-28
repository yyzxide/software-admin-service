package com.xcappstore.admin.controller;

import com.xcappstore.admin.common.ApiResponse;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping({"/health", "/api/v1/admin/health"})
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
            "service", "java-software-admin-service",
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }
}
