package com.xcappstore.admin.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewTaskCreateRequest {
    @NotNull(message = "软件ID不能为空")
    private Long appId;

    private Long versionId;

    @Size(max = 512, message = "提交原因不能超过512个字符")
    private String reason;

    @Min(value = 0, message = "优先级错误")
    @Max(value = 2, message = "优先级错误")
    private Integer priority;

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public void setApp_id(Long appId) { this.appId = appId; }
    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }
    public void setVersion_id(Long versionId) { this.versionId = versionId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}
