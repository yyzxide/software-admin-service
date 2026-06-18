package com.xcappstore.admin.auth.rbac.dto;

import java.util.List;

public class AdminRolePermissionsUpdateRequest {
    private List<Long> permissionIds;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }

    public void setPermission_ids(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
