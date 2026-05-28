package com.xcappstore.admin.category.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CategoryQueryRequest {
    private Long parentId;

    @Min(value = 0, message = "状态值错误")
    @Max(value = 1, message = "状态值错误")
    private Integer status;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public void setParent_id(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
