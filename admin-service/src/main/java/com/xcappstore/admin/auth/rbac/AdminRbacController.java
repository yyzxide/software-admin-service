package com.xcappstore.admin.auth.rbac;

import com.xcappstore.admin.auth.rbac.dto.AdminPasswordResetRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminPermissionResponse;
import com.xcappstore.admin.auth.rbac.dto.AdminRoleCreateRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminRolePermissionsUpdateRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminRoleResponse;
import com.xcappstore.admin.auth.rbac.dto.AdminRoleUpdateRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminUserCreateRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminUserManageResponse;
import com.xcappstore.admin.auth.rbac.dto.AdminUserRolesUpdateRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminUserStatusRequest;
import com.xcappstore.admin.auth.rbac.dto.AdminUserUpdateRequest;
import com.xcappstore.admin.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/rbac")
@RequirePermission("rbac:view")
public class AdminRbacController {
    private final AdminRbacService rbacService;

    public AdminRbacController(AdminRbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserManageResponse>> users(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @Min(0) @Max(1) Integer status
    ) {
        return ApiResponse.success(rbacService.listUsers(keyword, status));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserManageResponse> userDetail(@PathVariable Long id) {
        return ApiResponse.success(rbacService.detailUser(id));
    }

    @PostMapping("/users")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminUserManageResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        return ApiResponse.success(rbacService.createUser(request));
    }

    @PutMapping("/users/{id}")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminUserManageResponse> updateUser(
        @PathVariable Long id,
        @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        return ApiResponse.success(rbacService.updateUser(id, request));
    }

    @PostMapping("/users/{id}/status")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminUserManageResponse> updateUserStatus(
        @PathVariable Long id,
        @Valid @RequestBody AdminUserStatusRequest request
    ) {
        return ApiResponse.success(rbacService.updateUserStatus(id, request));
    }

    @PostMapping("/users/{id}/password")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminUserManageResponse> resetPassword(
        @PathVariable Long id,
        @Valid @RequestBody AdminPasswordResetRequest request
    ) {
        return ApiResponse.success(rbacService.resetPassword(id, request));
    }

    @PutMapping("/users/{id}/roles")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminUserManageResponse> updateUserRoles(
        @PathVariable Long id,
        @RequestBody AdminUserRolesUpdateRequest request
    ) {
        return ApiResponse.success(rbacService.updateUserRoles(id, request));
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleResponse>> roles(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) @Min(0) @Max(1) Integer status
    ) {
        return ApiResponse.success(rbacService.listRoles(keyword, status));
    }

    @GetMapping("/roles/{id}")
    public ApiResponse<AdminRoleResponse> roleDetail(@PathVariable Long id) {
        return ApiResponse.success(rbacService.detailRole(id));
    }

    @PostMapping("/roles")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminRoleResponse> createRole(@Valid @RequestBody AdminRoleCreateRequest request) {
        return ApiResponse.success(rbacService.createRole(request));
    }

    @PutMapping("/roles/{id}")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminRoleResponse> updateRole(
        @PathVariable Long id,
        @Valid @RequestBody AdminRoleUpdateRequest request
    ) {
        return ApiResponse.success(rbacService.updateRole(id, request));
    }

    @PutMapping("/roles/{id}/permissions")
    @RequirePermission("rbac:manage")
    public ApiResponse<AdminRoleResponse> updateRolePermissions(
        @PathVariable Long id,
        @RequestBody AdminRolePermissionsUpdateRequest request
    ) {
        return ApiResponse.success(rbacService.updateRolePermissions(id, request));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<AdminPermissionResponse>> permissions(
        @RequestParam(required = false) String module,
        @RequestParam(required = false) @Min(0) @Max(1) Integer status
    ) {
        return ApiResponse.success(rbacService.listPermissions(module, status));
    }
}
