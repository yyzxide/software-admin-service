package com.xcappstore.admin.auth.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public class AdminRoleCreateRequest {
    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$", message = "角色编码只能使用小写字母、数字和下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 128, message = "角色名称不能超过128个字符")
    private String roleName;

    @Size(max = 255, message = "角色说明不能超过255个字符")
    private String description;

    @Min(value = 0, message = "角色状态错误")
    @Max(value = 1, message = "角色状态错误")
    private Integer status;

    private List<Long> permissionIds;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public void setRole_code(String roleCode) {
        this.roleCode = roleCode;
    }

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
