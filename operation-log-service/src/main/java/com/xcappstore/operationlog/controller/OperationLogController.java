package com.xcappstore.operationlog.controller;

import com.xcappstore.operationlog.common.ApiResponse;
import com.xcappstore.operationlog.common.PageResponse;
import com.xcappstore.operationlog.dto.OperationLogOptionsResponse;
import com.xcappstore.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.operationlog.dto.OperationLogResponse;
import com.xcappstore.operationlog.dto.OperationLogStatsResponse;
import com.xcappstore.operationlog.service.OperationLogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/operation-logs")
public class OperationLogController {
    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OperationLogResponse>> list(@Valid OperationLogQueryRequest request) {
        return ApiResponse.success(operationLogService.list(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<OperationLogResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(operationLogService.detail(id));
    }

    @GetMapping("/options")
    public ApiResponse<OperationLogOptionsResponse> options() {
        return ApiResponse.success(operationLogService.options());
    }

    @GetMapping("/stats")
    public ApiResponse<OperationLogStatsResponse> stats(@Valid OperationLogQueryRequest request) {
        return ApiResponse.success(operationLogService.stats(request));
    }
}
