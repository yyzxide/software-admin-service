package com.xcappstore.admin.software.controller;

import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareStatusChangeRequest;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.service.SoftwareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/software/apps")
public class SoftwareController {
    private final SoftwareService softwareService;

    public SoftwareController(SoftwareService softwareService) {
        this.softwareService = softwareService;
    }

    @GetMapping
    public ApiResponse<PageResponse<SoftwareResponse>> page(@Valid SoftwareQueryRequest request) {
        return ApiResponse.success(softwareService.page(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SoftwareResponse> upload(
        @Valid @ModelAttribute SoftwareUploadRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.upload(request, adminUserId(servletRequest)));
    }

    @GetMapping("/{id}")
    public ApiResponse<SoftwareResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(softwareService.detail(id));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<SoftwareResponse> publish(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) SoftwareStatusChangeRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.publish(id, adminUserId(servletRequest)));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<SoftwareResponse> unpublish(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) SoftwareStatusChangeRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.unpublish(id, adminUserId(servletRequest)));
    }

    private Long adminUserId(HttpServletRequest request) {
        Object principal = request.getAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR);
        if (principal instanceof AdminPrincipal adminPrincipal) {
            return adminPrincipal.getUserId();
        }
        try {
            return Long.parseLong(request.getHeader("X-User-ID"));
        } catch (Exception ex) {
            return 0L;
        }
    }
}
