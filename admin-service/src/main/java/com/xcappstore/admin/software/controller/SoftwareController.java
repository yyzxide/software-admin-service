package com.xcappstore.admin.software.controller;

import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.PackageAppendRequest;
import com.xcappstore.admin.software.dto.PackageScanRequest;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareStatusChangeRequest;
import com.xcappstore.admin.software.dto.SoftwareUpdateRequest;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.dto.VersionCreateRequest;
import com.xcappstore.admin.software.service.SoftwareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/software/apps")
@RequirePermission("software:view")
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
    @RequirePermission("software:create")
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

    @PutMapping("/{id}")
    @RequirePermission("software:update")
    public ApiResponse<SoftwareResponse> update(
        @PathVariable Long id,
        @Valid @RequestBody SoftwareUpdateRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.update(id, request, adminUserId(servletRequest)));
    }

    @PostMapping("/{id}/publish")
    @RequirePermission("software:publish")
    public ApiResponse<SoftwareResponse> publish(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) SoftwareStatusChangeRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.publish(id, adminUserId(servletRequest)));
    }

    @PostMapping("/{id}/unpublish")
    @RequirePermission("software:unpublish")
    public ApiResponse<SoftwareResponse> unpublish(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) SoftwareStatusChangeRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.unpublish(id, adminUserId(servletRequest)));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("software:version:create")
    public ApiResponse<AppVersionResponse> addVersion(
        @PathVariable Long id,
        @Valid @ModelAttribute VersionCreateRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.addVersion(id, request, adminUserId(servletRequest)));
    }

    @PostMapping(value = "/{id}/versions/{versionId}/packages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("software:package:create")
    public ApiResponse<AppPackageResponse> addPackage(
        @PathVariable Long id,
        @PathVariable Long versionId,
        @Valid @ModelAttribute PackageAppendRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.addPackage(id, versionId, request, adminUserId(servletRequest)));
    }

    @PostMapping("/{id}/packages/{packageId}/scan")
    @RequirePermission("software:package:scan")
    public ApiResponse<AppPackageResponse> scanPackage(
        @PathVariable Long id,
        @PathVariable Long packageId,
        @Valid @RequestBody(required = false) PackageScanRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(softwareService.scanPackage(id, packageId, request, adminUserId(servletRequest)));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<AppVersionResponse>> versions(@PathVariable Long id) {
        return ApiResponse.success(softwareService.versions(id));
    }

    @GetMapping("/{id}/packages")
    public ApiResponse<List<AppPackageResponse>> packages(@PathVariable Long id) {
        return ApiResponse.success(softwareService.packages(id));
    }

    private Long adminUserId(HttpServletRequest request) {
        Object principal = request.getAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR);
        if (principal instanceof AdminPrincipal adminPrincipal) {
            return adminPrincipal.getUserId();
        }
        return 0L;
    }
}
