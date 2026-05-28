package com.xcappstore.admin.tag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class TagQueryRequest {
    @Min(value = 0, message = "热门标签状态错误")
    @Max(value = 1, message = "热门标签状态错误")
    private Integer isHot;

    @Size(max = 50, message = "关键词不能超过50个字符")
    private String keyword;

    public Integer getIsHot() {
        return isHot;
    }

    public void setIsHot(Integer isHot) {
        this.isHot = isHot;
    }

    public void setIs_hot(Integer isHot) {
        this.isHot = isHot;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
