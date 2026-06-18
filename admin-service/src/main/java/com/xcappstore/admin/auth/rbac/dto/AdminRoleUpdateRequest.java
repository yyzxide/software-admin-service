package com.xcappstore.admin.auth.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AdminRoleUpdateRequest {
    @Size(max = 128, message = "角色名称不能超过128个字符")
    private String roleName;

    @Size(max = 255, message = "角色说明不能超过255个字符")
    private String description;

    @Min(value = 0, message = "角色状态错误")
    @Max(value = 1, message = "角色状态错误")
    private Integer status;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setRole_name(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
