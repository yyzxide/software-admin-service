package com.xcappstore.admin.review.dto;

import jakarta.validation.constraints.NotNull;

public class ReviewAssignRequest {
    @NotNull(message = "审核人ID不能为空")
    private Long reviewerId;

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public void setReviewer_id(Long reviewerId) { this.reviewerId = reviewerId; }
}
