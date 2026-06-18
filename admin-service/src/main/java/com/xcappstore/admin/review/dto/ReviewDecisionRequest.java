package com.xcappstore.admin.review.dto;

import jakarta.validation.constraints.Size;

public class ReviewDecisionRequest {
    @Size(max = 512, message = "审核意见不能超过512个字符")
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
