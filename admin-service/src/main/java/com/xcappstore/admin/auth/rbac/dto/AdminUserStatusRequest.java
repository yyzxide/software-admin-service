package com.xcappstore.admin.auth.rbac.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class AdminUserStatusRequest {
    @Min(value = 0, message = "账号状态错误")
    @Max(value = 1, message = "账号状态错误")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
