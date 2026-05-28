package com.xcappstore.admin.auth.controller;

import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.auth.AdminTokenService;
import com.xcappstore.admin.auth.dto.AdminUserResponse;
import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
    private final AdminTokenService adminTokenService;

    public AdminAuthController(AdminTokenService adminTokenService) {
        this.adminTokenService = adminTokenService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(adminTokenService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminUserResponse> me(HttpServletRequest request) {
        AdminPrincipal principal = (AdminPrincipal) request.getAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR);
        return ApiResponse.success(adminTokenService.toUser(principal));
    }
}
