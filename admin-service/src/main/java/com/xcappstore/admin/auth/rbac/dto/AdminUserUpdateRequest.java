package com.xcappstore.admin.auth.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AdminUserUpdateRequest {
    @Size(max = 128, message = "显示名称不能超过128个字符")
    private String displayName;

    @Min(value = 0, message = "账号状态错误")
    @Max(value = 1, message = "账号状态错误")
    private Integer status;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDisplay_name(String displayName) {
        this.displayName = displayName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
