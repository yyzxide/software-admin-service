package com.xcappstore.admin.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ReviewTaskQueryRequest {
    private Integer status;
    private Long appId;
    private Long reviewerId;
    private String keyword;

    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 20;

    public int offset() { return (normalizedPage() - 1) * normalizedPageSize(); }
    public int normalizedPage() { return page == null || page < 1 ? 1 : page; }
    public int normalizedPageSize() { return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100); }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }
    public void setApp_id(Long appId) { this.appId = appId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public void setReviewer_id(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public void setPage_size(Integer pageSize) { this.pageSize = pageSize; }
}
