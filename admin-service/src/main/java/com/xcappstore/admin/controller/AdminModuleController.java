package com.xcappstore.admin.controller;

import com.xcappstore.admin.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/modules")
public class AdminModuleController {
    @GetMapping
    public ApiResponse<Map<String, Object>> modules() {
        return ApiResponse.success(Map.of(
            "service", "java-software-admin-service",
            "modules", List.of("category", "tag", "software", "version", "packageinfo", "operationlog")
        ));
    }
}
