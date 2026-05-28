package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.Size;

public class SoftwareStatusChangeRequest {
    @Size(max = 255, message = "操作原因不能超过255个字符")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
