package com.xcappstore.admin.auth.rbac;

import com.xcappstore.admin.auth.AdminPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminPermissionService {
    private static final String ALL_PERMISSIONS = "*";
    private static final String ADMIN_USER_TYPE = "admin";

    private final AdminPermissionMapper permissionMapper;

    public AdminPermissionService(AdminPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    public boolean hasPermission(AdminPrincipal principal, String requiredPermission) {
        if (principal == null || !ADMIN_USER_TYPE.equals(principal.getUserType())) {
            return false;
        }
        if (!StringUtils.hasText(requiredPermission)) {
            return true;
        }
        if (principal.getUserId() == null || principal.getUserId() <= 0) {
            return false;
        }

        List<String> permissions = permissionMapper.selectPermissionCodesByUserId(principal.getUserId());
        return permissions.stream().anyMatch(permission -> matches(permission, requiredPermission));
    }

    private boolean matches(String grantedPermission, String requiredPermission) {
        if (!StringUtils.hasText(grantedPermission)) {
            return false;
        }
        if (ALL_PERMISSIONS.equals(grantedPermission) || grantedPermission.equals(requiredPermission)) {
            return true;
        }
        int separatorIndex = grantedPermission.indexOf(':');
        if (separatorIndex <= 0 || !grantedPermission.endsWith(":*")) {
            return false;
        }
        String grantedModule = grantedPermission.substring(0, separatorIndex);
        String requiredModule = requiredPermission.contains(":")
            ? requiredPermission.substring(0, requiredPermission.indexOf(':'))
            : requiredPermission;
        return grantedModule.equals(requiredModule);
    }
}
