package com.xcappstore.admin.auth.rbac.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class AdminUserCreateRequest {
    @NotBlank(message = "管理员账号不能为空")
    @Size(max = 64, message = "管理员账号不能超过64个字符")
    private String username;

    @Size(max = 128, message = "显示名称不能超过128个字符")
    private String displayName;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 64, message = "登录密码长度必须在6到64个字符之间")
    private String password;

    private List<Long> roleIds;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplay_name(String displayName) {
        this.displayName = displayName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
