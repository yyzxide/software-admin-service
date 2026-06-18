package com.xcappstore.admin.review.controller;

import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.review.dto.ReviewAssignRequest;
import com.xcappstore.admin.review.dto.ReviewDecisionRequest;
import com.xcappstore.admin.review.dto.ReviewTaskCreateRequest;
import com.xcappstore.admin.review.dto.ReviewTaskQueryRequest;
import com.xcappstore.admin.review.dto.ReviewTaskResponse;
import com.xcappstore.admin.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequirePermission("review:view")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @RequirePermission("review:submit")
    public ApiResponse<ReviewTaskResponse> create(
        @Valid @RequestBody ReviewTaskCreateRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(reviewService.create(request, adminUserId(servletRequest)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewTaskResponse>> list(@Valid ReviewTaskQueryRequest request) {
        return ApiResponse.success(reviewService.list(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewTaskResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(reviewService.detail(id));
    }

    @PostMapping("/{id}/assign")
    @RequirePermission("review:assign")
    public ApiResponse<ReviewTaskResponse> assign(
        @PathVariable Long id,
        @Valid @RequestBody ReviewAssignRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(reviewService.assign(id, request, adminUserId(servletRequest)));
    }

    @PostMapping("/{id}/approve")
    @RequirePermission("review:approve")
    public ApiResponse<ReviewTaskResponse> approve(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ReviewDecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(reviewService.approve(id, request, adminUserId(servletRequest)));
    }

    @PostMapping("/{id}/reject")
    @RequirePermission("review:reject")
    public ApiResponse<ReviewTaskResponse> reject(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) ReviewDecisionRequest request,
        HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(reviewService.reject(id, request, adminUserId(servletRequest)));
    }

    private Long adminUserId(HttpServletRequest request) {
        Object principal = request.getAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR);
        if (principal instanceof AdminPrincipal adminPrincipal) {
            return adminPrincipal.getUserId();
        }
        return 0L;
    }
}
