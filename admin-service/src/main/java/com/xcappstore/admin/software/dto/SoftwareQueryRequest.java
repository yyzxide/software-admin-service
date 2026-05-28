package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class SoftwareQueryRequest {
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 20;

    @Size(max = 100, message = "关键词不能超过100个字符")
    private String keyword;

    private Long categoryId;

    private Long developerId;

    @Min(value = 0, message = "软件状态错误")
    @Max(value = 4, message = "软件状态错误")
    private Integer status;

    @Min(value = 0, message = "官方状态错误")
    @Max(value = 1, message = "官方状态错误")
    private Integer isOfficial;

    @Min(value = 0, message = "推荐状态错误")
    @Max(value = 1, message = "推荐状态错误")
    private Integer isFeatured;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public void setPage_size(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategory_id(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(Long developerId) {
        this.developerId = developerId;
    }

    public void setDeveloper_id(Long developerId) {
        this.developerId = developerId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Integer isOfficial) {
        this.isOfficial = isOfficial;
    }

    public void setIs_official(Integer isOfficial) {
        this.isOfficial = isOfficial;
    }

    public Integer getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Integer isFeatured) {
        this.isFeatured = isFeatured;
    }

    public void setIs_featured(Integer isFeatured) {
        this.isFeatured = isFeatured;
    }

    public int offset() {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        return (safePage - 1) * safePageSize;
    }
}
