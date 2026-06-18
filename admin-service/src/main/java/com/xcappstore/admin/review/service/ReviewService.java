package com.xcappstore.admin.review.service;

import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.review.dto.ReviewAssignRequest;
import com.xcappstore.admin.review.dto.ReviewDecisionRequest;
import com.xcappstore.admin.review.dto.ReviewTaskCreateRequest;
import com.xcappstore.admin.review.dto.ReviewTaskQueryRequest;
import com.xcappstore.admin.review.dto.ReviewTaskResponse;

public interface ReviewService {
    ReviewTaskResponse create(ReviewTaskCreateRequest request, Long adminUserId);

    PageResponse<ReviewTaskResponse> list(ReviewTaskQueryRequest request);

    ReviewTaskResponse detail(Long id);

    ReviewTaskResponse assign(Long id, ReviewAssignRequest request, Long adminUserId);

    ReviewTaskResponse approve(Long id, ReviewDecisionRequest request, Long adminUserId);

    ReviewTaskResponse reject(Long id, ReviewDecisionRequest request, Long adminUserId);
}
