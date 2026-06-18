package com.xcappstore.admin.auth.rbac.dto;

import java.util.List;

public class AdminUserRolesUpdateRequest {
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    public void setRole_ids(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
