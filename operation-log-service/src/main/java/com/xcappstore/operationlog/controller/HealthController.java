package com.xcappstore.operationlog.controller;

import com.xcappstore.operationlog.common.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "operation-log-service");
        data.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(data);
    }
}
