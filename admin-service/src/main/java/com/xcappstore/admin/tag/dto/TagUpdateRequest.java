package com.xcappstore.admin.tag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class TagUpdateRequest {
    @Size(max = 50, message = "标签名称不能超过50个字符")
    private String name;

    @Size(max = 255, message = "标签描述不能超过255个字符")
    private String description;

    @Min(value = 0, message = "热门标签状态错误")
    @Max(value = 1, message = "热门标签状态错误")
    private Integer isHot;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIsHot() {
        return isHot;
    }

    public void setIsHot(Integer isHot) {
        this.isHot = isHot;
    }

    public void setIs_hot(Integer isHot) {
        this.isHot = isHot;
    }
}
