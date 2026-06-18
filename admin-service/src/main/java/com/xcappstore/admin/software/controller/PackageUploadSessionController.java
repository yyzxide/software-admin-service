package com.xcappstore.admin.software.controller;

import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.software.dto.PackageUploadChunkRequest;
import com.xcappstore.admin.software.dto.PackageUploadCreateRequest;
import com.xcappstore.admin.software.dto.PackageUploadSessionResponse;
import com.xcappstore.admin.software.service.PackageUploadSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/software/package-upload-sessions")
@RequirePermission("software:upload")
public class PackageUploadSessionController {
    private final PackageUploadSessionService uploadSessionService;

    public PackageUploadSessionController(PackageUploadSessionService uploadSessionService) {
        this.uploadSessionService = uploadSessionService;
    }

    @PostMapping
    public ApiResponse<PackageUploadSessionResponse> create(
        @Valid @RequestBody PackageUploadCreateRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(uploadSessionService.create(request, adminUserId(servletRequest)));
    }

    @PostMapping(value = "/{uploadId}/chunks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PackageUploadSessionResponse> uploadChunk(
        @PathVariable String uploadId,
        @Valid @ModelAttribute PackageUploadChunkRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(uploadSessionService.uploadChunk(uploadId, request, adminUserId(servletRequest)));
    }

    @GetMapping("/{uploadId}")
    public ApiResponse<PackageUploadSessionResponse> status(
        @PathVariable String uploadId,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(uploadSessionService.status(uploadId, adminUserId(servletRequest)));
    }

    @PostMapping("/{uploadId}/complete")
    public ApiResponse<PackageUploadSessionResponse> complete(
        @PathVariable String uploadId,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(uploadSessionService.complete(uploadId, adminUserId(servletRequest)));
    }

    private Long adminUserId(HttpServletRequest request) {
        Object principal = request.getAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR);
        if (principal instanceof AdminPrincipal adminPrincipal) {
            return adminPrincipal.getUserId();
        }
        return 0L;
    }
}
