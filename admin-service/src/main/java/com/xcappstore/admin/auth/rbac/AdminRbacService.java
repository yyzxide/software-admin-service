package com.xcappstore.admin.auth.rbac;

import com.xcappstore.admin.auth.PasswordHashService;
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
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminRbacService {
    private static final long SUPER_ADMIN_USER_ID = 1L;

    private final AdminRbacMapper mapper;
    private final PasswordHashService passwordHashService;

    public AdminRbacService(AdminRbacMapper mapper, PasswordHashService passwordHashService) {
        this.mapper = mapper;
        this.passwordHashService = passwordHashService;
    }

    public List<AdminUserManageResponse> listUsers(String keyword, Integer status) {
        return mapper.selectUsers(normalizeText(keyword), status).stream()
            .map(this::toUserResponse)
            .toList();
    }

    @Transactional
    public AdminUserManageResponse createUser(AdminUserCreateRequest request) {
        String username = normalizeRequired(request.getUsername(), "管理员账号不能为空");
        if (mapper.countUsersByUsername(username, null) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "管理员账号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        AdminUserEntity user = new AdminUserEntity();
        user.setUsername(username);
        user.setDisplayName(defaultText(request.getDisplayName(), username));
        user.setPasswordHash(passwordHashService.hash(request.getPassword()));
        user.setStatus(1);
        user.setTokenVersion(0L);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        mapper.insertUser(user);
        replaceUserRoles(user.getId(), request.getRoleIds());
        return detailUser(user.getId());
    }

    @Transactional
    public AdminUserManageResponse updateUser(Long id, AdminUserUpdateRequest request) {
        AdminUserEntity user = requireUser(id);
        user.setDisplayName(defaultText(request.getDisplayName(), user.getDisplayName()));
        user.setStatus(request.getStatus() == null ? user.getStatus() : request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        if (SUPER_ADMIN_USER_ID == user.getId() && Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "不能禁用内置超级管理员");
        }
        mapper.updateUser(user);
        return detailUser(id);
    }

    @Transactional
    public AdminUserManageResponse updateUserStatus(Long id, AdminUserStatusRequest request) {
        AdminUserEntity user = requireUser(id);
        Integer status = request.getStatus();
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "账号状态不能为空");
        }
        if (SUPER_ADMIN_USER_ID == user.getId() && status == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "不能禁用内置超级管理员");
        }
        mapper.updateUserStatus(id, status);
        return detailUser(id);
    }

    @Transactional
    public AdminUserManageResponse resetPassword(Long id, AdminPasswordResetRequest request) {
        requireUser(id);
        mapper.updateUserPassword(id, passwordHashService.hash(request.getPassword()));
        return detailUser(id);
    }

    @Transactional
    public AdminUserManageResponse updateUserRoles(Long id, AdminUserRolesUpdateRequest request) {
        requireUser(id);
        replaceUserRoles(id, request.getRoleIds());
        return detailUser(id);
    }

    public AdminUserManageResponse detailUser(Long id) {
        return toUserResponse(requireUser(id));
    }

    public List<AdminRoleResponse> listRoles(String keyword, Integer status) {
        return mapper.selectRoles(normalizeText(keyword), status).stream()
            .map(this::toRoleResponse)
            .toList();
    }

    @Transactional
    public AdminRoleResponse createRole(AdminRoleCreateRequest request) {
        String roleCode = normalizeRequired(request.getRoleCode(), "角色编码不能为空");
        if (mapper.countRolesByCode(roleCode, null) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "角色编码已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        AdminRoleEntity role = new AdminRoleEntity();
        role.setRoleCode(roleCode);
        role.setRoleName(normalizeRequired(request.getRoleName(), "角色名称不能为空"));
        role.setDescription(normalizeText(request.getDescription()));
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        mapper.insertRole(role);
        replaceRolePermissions(role.getId(), request.getPermissionIds());
        return detailRole(role.getId());
    }

    @Transactional
    public AdminRoleResponse updateRole(Long id, AdminRoleUpdateRequest request) {
        AdminRoleEntity role = requireRole(id);
        role.setRoleName(defaultText(request.getRoleName(), role.getRoleName()));
        role.setDescription(normalizeText(request.getDescription()));
        role.setStatus(request.getStatus() == null ? role.getStatus() : request.getStatus());
        role.setUpdatedAt(LocalDateTime.now());
        mapper.updateRole(role);
        return detailRole(id);
    }

    @Transactional
    public AdminRoleResponse updateRolePermissions(Long id, AdminRolePermissionsUpdateRequest request) {
        requireRole(id);
        replaceRolePermissions(id, request.getPermissionIds());
        return detailRole(id);
    }

    public AdminRoleResponse detailRole(Long id) {
        return toRoleResponse(requireRole(id));
    }

    public List<AdminPermissionResponse> listPermissions(String module, Integer status) {
        return mapper.selectPermissions(normalizeText(module), status).stream()
            .map(this::toPermissionResponse)
            .toList();
    }

    private AdminUserEntity requireUser(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "管理员ID非法");
        }
        AdminUserEntity user = mapper.selectUserById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "管理员不存在");
        }
        return user;
    }

    private AdminRoleEntity requireRole(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "角色ID非法");
        }
        AdminRoleEntity role = mapper.selectRoleById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private void replaceUserRoles(Long userId, List<Long> roleIds) {
        List<Long> normalizedRoleIds = normalizeIds(roleIds);
        if (!normalizedRoleIds.isEmpty() && mapper.countRolesByIds(normalizedRoleIds) != normalizedRoleIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "存在无效或禁用的角色ID");
        }
        mapper.deleteUserRoles(userId);
        for (Long roleId : normalizedRoleIds) {
            mapper.insertUserRole(userId, roleId);
        }
    }

    private void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        List<Long> normalizedPermissionIds = normalizeIds(permissionIds);
        if (!normalizedPermissionIds.isEmpty()
            && mapper.countPermissionsByIds(normalizedPermissionIds) != normalizedPermissionIds.size()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "存在无效或禁用的权限ID");
        }
        mapper.deleteRolePermissions(roleId);
        for (Long permissionId : normalizedPermissionIds) {
            mapper.insertRolePermission(roleId, permissionId);
        }
    }

    private AdminUserManageResponse toUserResponse(AdminUserEntity user) {
        AdminUserManageResponse response = new AdminUserManageResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setRoles(mapper.selectRolesByUserId(user.getId()).stream().map(this::toRoleSummary).toList());
        return response;
    }

    private AdminRoleResponse toRoleResponse(AdminRoleEntity role) {
        AdminRoleResponse response = toRoleSummary(role);
        response.setPermissions(mapper.selectPermissionsByRoleId(role.getId()).stream().map(this::toPermissionResponse).toList());
        return response;
    }

    private AdminRoleResponse toRoleSummary(AdminRoleEntity role) {
        AdminRoleResponse response = new AdminRoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setStatus(role.getStatus());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }

    private AdminPermissionResponse toPermissionResponse(AdminPermissionEntity permission) {
        AdminPermissionResponse response = new AdminPermissionResponse();
        response.setId(permission.getId());
        response.setPermissionCode(permission.getPermissionCode());
        response.setPermissionName(permission.getPermissionName());
        response.setModule(permission.getModule());
        response.setDescription(permission.getDescription());
        response.setStatus(permission.getStatus());
        return response;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                deduplicated.add(id);
            }
        }
        return new ArrayList<>(deduplicated);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultText(String value, String fallback) {
        String normalized = normalizeText(value);
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }
}
