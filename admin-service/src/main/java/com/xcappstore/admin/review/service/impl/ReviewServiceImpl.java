package com.xcappstore.admin.review.service.impl;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.service.OperationLogPublisher;
import com.xcappstore.admin.review.dto.ReviewAssignRequest;
import com.xcappstore.admin.review.dto.ReviewDecisionRequest;
import com.xcappstore.admin.review.dto.ReviewHistoryResponse;
import com.xcappstore.admin.review.dto.ReviewTaskCreateRequest;
import com.xcappstore.admin.review.dto.ReviewTaskQueryRequest;
import com.xcappstore.admin.review.dto.ReviewTaskResponse;
import com.xcappstore.admin.review.entity.ReviewHistoryEntity;
import com.xcappstore.admin.review.entity.ReviewTaskEntity;
import com.xcappstore.admin.review.mapper.ReviewMapper;
import com.xcappstore.admin.review.model.ReviewTaskStatus;
import com.xcappstore.admin.review.service.ReviewService;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import com.xcappstore.admin.software.model.SoftwareStatus;
import com.xcappstore.admin.software.model.AppVersionStatus;
import com.xcappstore.admin.software.service.PackageSecurityPolicyService;
import com.xcappstore.admin.software.service.SoftwareCacheService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewServiceImpl implements ReviewService {
    private static final String TARGET_SOFTWARE = "software";
    private static final String TARGET_VERSION = "version";

    private final ReviewMapper reviewMapper;
    private final SoftwareMapper softwareMapper;
    private final PackageSecurityPolicyService packageSecurityPolicyService;
    private final SoftwareCacheService softwareCacheService;
    private final OperationLogPublisher operationLogPublisher;

    public ReviewServiceImpl(
        ReviewMapper reviewMapper,
        SoftwareMapper softwareMapper,
        PackageSecurityPolicyService packageSecurityPolicyService,
        SoftwareCacheService softwareCacheService,
        OperationLogPublisher operationLogPublisher
    ) {
        this.reviewMapper = reviewMapper;
        this.softwareMapper = softwareMapper;
        this.packageSecurityPolicyService = packageSecurityPolicyService;
        this.softwareCacheService = softwareCacheService;
        this.operationLogPublisher = operationLogPublisher;
    }

    @Override
    @Transactional
    public ReviewTaskResponse create(ReviewTaskCreateRequest request, Long adminUserId) {
        Long operatorId = normalizeAdminUserId(adminUserId);
        SoftwareEntity app = requireSoftware(request.getAppId());
        AppVersionEntity version = null;
        if (request.getVersionId() != null) {
            version = requireVersion(app.getId(), request.getVersionId());
            if (!AppVersionStatus.fromCode(version.getStatus()).canSubmitReview()) {
                throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "当前版本状态不允许提交审核");
            }
        }
        if (version == null && !SoftwareStatus.fromCode(app.getStatus()).canSubmitReview()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "当前软件状态不允许提交审核");
        }
        if (reviewMapper.countActiveTask(app.getId(), version == null ? null : version.getId()) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该软件存在待处理审核任务");
        }

        LocalDateTime now = LocalDateTime.now();
        ReviewTaskEntity task = new ReviewTaskEntity();
        task.setAppId(app.getId());
        task.setVersionId(version == null ? null : version.getId());
        task.setTargetType(version == null ? TARGET_SOFTWARE : TARGET_VERSION);
        task.setTitle(version == null ? app.getName() + " 上架审核" : app.getName() + " " + version.getVersionName() + " 版本审核");
        task.setStatus(ReviewTaskStatus.PENDING.code());
        task.setPriority(request.getPriority() == null ? 1 : request.getPriority());
        task.setSubmitReason(normalizeText(request.getReason()));
        task.setSubmittedBy(operatorId);
        task.setSubmittedAt(now);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        try {
            reviewMapper.insertTask(task);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该软件存在待处理审核任务");
        }
        insertHistory(task.getId(), "submit", null, ReviewTaskStatus.PENDING.code(), operatorId, request.getReason(), now);

        if (version == null) {
            ensureReviewTargetUpdated(softwareMapper.updateAppReviewing(app.getId(), operatorId));
            softwareMapper.updateDraftVersionsReviewing(app.getId(), operatorId);
        } else {
            ensureReviewTargetUpdated(softwareMapper.updateVersionReviewing(version.getId(), operatorId));
        }
        softwareCacheService.invalidateDetail(app.getId());
        operationLogPublisher.record(operatorId, "submit_review", "review_task", task.getId(), task.getTitle(), "提交审核任务: " + task.getTitle());
        return detail(task.getId());
    }

    @Override
    public PageResponse<ReviewTaskResponse> list(ReviewTaskQueryRequest request) {
        normalizeQuery(request);
        long total = reviewMapper.count(request);
        List<ReviewTaskResponse> list = total == 0 ? List.of() : reviewMapper
            .selectPage(request, request.offset(), request.normalizedPageSize())
            .stream()
            .map(this::toResponseWithoutHistories)
            .toList();
        return new PageResponse<>(total, request.normalizedPage(), request.normalizedPageSize(), list);
    }

    @Override
    public ReviewTaskResponse detail(Long id) {
        ReviewTaskEntity task = requireTask(id);
        ReviewTaskResponse response = toResponseWithoutHistories(task);
        response.setHistories(reviewMapper.selectHistories(id).stream().map(this::toHistoryResponse).toList());
        return response;
    }

    @Override
    @Transactional
    public ReviewTaskResponse assign(Long id, ReviewAssignRequest request, Long adminUserId) {
        ReviewTaskEntity task = requireTask(id);
        ensureDecidable(task);
        Long operatorId = normalizeAdminUserId(adminUserId);
        if (request.getReviewerId() == null || request.getReviewerId() <= 0
            || reviewMapper.countEligibleReviewer(request.getReviewerId()) == 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "审核人不存在、已禁用或缺少审核权限");
        }
        LocalDateTime now = LocalDateTime.now();
        int affected = reviewMapper.assign(id, request.getReviewerId(), ReviewTaskStatus.REVIEWING.code(), now);
        ensureUpdated(affected);
        insertHistory(id, "assign", task.getStatus(), ReviewTaskStatus.REVIEWING.code(), operatorId, "分配审核人: " + request.getReviewerId(), now);
        operationLogPublisher.record(operatorId, "assign_review", "review_task", id, task.getTitle(), "分配审核任务: " + task.getTitle());
        return detail(id);
    }

    @Override
    @Transactional
    public ReviewTaskResponse approve(Long id, ReviewDecisionRequest request, Long adminUserId) {
        ReviewTaskEntity task = requireTask(id);
        ensureDecidable(task);
        Long operatorId = normalizeAdminUserId(adminUserId);
        ensureReviewerOwnership(task, operatorId);
        if (task.getVersionId() != null) {
            packageSecurityPolicyService.assertVersionPackagesPublishable(task.getAppId(), task.getVersionId());
        } else {
            packageSecurityPolicyService.assertAppPackagesPublishable(task.getAppId());
        }
        LocalDateTime now = LocalDateTime.now();
        String comment = defaultText(normalizeText(request == null ? null : request.getComment()), "审核通过");
        int affected = reviewMapper.finish(id, ReviewTaskStatus.APPROVED.code(), comment, operatorId, now, now);
        ensureUpdated(affected);
        insertHistory(id, "approve", task.getStatus(), ReviewTaskStatus.APPROVED.code(), operatorId, comment, now);
        if (task.getVersionId() != null) {
            softwareMapper.markVersionsNotLatest(task.getAppId(), operatorId);
            softwareMapper.approveVersion(task.getVersionId(), now, now, operatorId);
        } else {
            softwareMapper.approveDraftVersions(task.getAppId(), now, now, operatorId);
        }
        softwareMapper.updateStatus(task.getAppId(), SoftwareStatus.PUBLISHED.code(), now, operatorId);
        softwareCacheService.invalidateDetail(task.getAppId());
        operationLogPublisher.record(operatorId, "approve_review", "review_task", id, task.getTitle(), "审核通过: " + task.getTitle());
        return detail(id);
    }

    @Override
    @Transactional
    public ReviewTaskResponse reject(Long id, ReviewDecisionRequest request, Long adminUserId) {
        ReviewTaskEntity task = requireTask(id);
        ensureDecidable(task);
        Long operatorId = normalizeAdminUserId(adminUserId);
        ensureReviewerOwnership(task, operatorId);
        LocalDateTime now = LocalDateTime.now();
        String comment = defaultText(normalizeText(request == null ? null : request.getComment()), "审核驳回");
        int affected = reviewMapper.finish(id, ReviewTaskStatus.REJECTED.code(), comment, operatorId, now, now);
        ensureUpdated(affected);
        insertHistory(id, "reject", task.getStatus(), ReviewTaskStatus.REJECTED.code(), operatorId, comment, now);
        if (task.getVersionId() != null) {
            softwareMapper.rejectVersion(task.getVersionId(), now, operatorId);
        } else {
            softwareMapper.rejectAppReview(task.getAppId(), now, operatorId);
            softwareMapper.rejectDraftVersions(task.getAppId(), now, operatorId);
        }
        softwareCacheService.invalidateDetail(task.getAppId());
        operationLogPublisher.record(operatorId, "reject_review", "review_task", id, task.getTitle(), "审核驳回: " + task.getTitle());
        return detail(id);
    }

    private SoftwareEntity requireSoftware(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "软件ID错误");
        }
        SoftwareEntity app = softwareMapper.selectById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.SOFTWARE_NOT_FOUND, "软件不存在");
        }
        return app;
    }

    private AppVersionEntity requireVersion(Long appId, Long versionId) {
        if (versionId == null || versionId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "版本ID错误");
        }
        AppVersionEntity version = softwareMapper.selectVersionById(versionId);
        if (version == null || !appId.equals(version.getAppId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "版本不存在");
        }
        return version;
    }

    private ReviewTaskEntity requireTask(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "参数格式错误");
        }
        ReviewTaskEntity task = reviewMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.REVIEW_TASK_NOT_FOUND, "审核任务不存在");
        }
        return task;
    }

    private void ensureDecidable(ReviewTaskEntity task) {
        if (!ReviewTaskStatus.fromCode(task.getStatus()).canDecide()) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS, "当前审核任务状态不允许操作");
        }
    }

    private void ensureUpdated(int affected) {
        if (affected == 0) {
            throw new BusinessException(ErrorCode.REVIEW_INVALID_STATUS, "审核任务已被其他人处理");
        }
    }

    private void ensureReviewTargetUpdated(int affected) {
        if (affected == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "审核目标状态已变化，请刷新后重试");
        }
    }

    private void ensureReviewerOwnership(ReviewTaskEntity task, Long operatorId) {
        if (task.getReviewerId() != null && !task.getReviewerId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "审核任务已分配给其他审核人");
        }
    }

    private void insertHistory(Long taskId, String action, Integer fromStatus, Integer toStatus, Long operatorId, String comment, LocalDateTime now) {
        ReviewHistoryEntity history = new ReviewHistoryEntity();
        history.setTaskId(taskId);
        history.setAction(action);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setOperatorId(operatorId);
        history.setComment(normalizeText(comment));
        history.setCreatedAt(now);
        reviewMapper.insertHistory(history);
    }

    private void normalizeQuery(ReviewTaskQueryRequest request) {
        if (StringUtils.hasText(request.getKeyword())) {
            request.setKeyword(request.getKeyword().trim());
        }
    }

    private Long normalizeAdminUserId(Long adminUserId) {
        return adminUserId == null || adminUserId <= 0 ? 0L : adminUserId;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private ReviewTaskResponse toResponseWithoutHistories(ReviewTaskEntity task) {
        ReviewTaskResponse response = new ReviewTaskResponse();
        response.setId(task.getId());
        response.setAppId(task.getAppId());
        response.setVersionId(task.getVersionId());
        response.setTargetType(task.getTargetType());
        response.setAppName(task.getAppName());
        response.setVersionName(task.getVersionName());
        response.setTitle(task.getTitle());
        response.setStatus(task.getStatus());
        response.setStatusText(ReviewTaskStatus.fromCode(task.getStatus()).text());
        response.setPriority(task.getPriority());
        response.setPriorityText(priorityText(task.getPriority()));
        response.setSubmitReason(task.getSubmitReason());
        response.setReviewComment(task.getReviewComment());
        response.setReviewerId(task.getReviewerId());
        response.setSubmittedBy(task.getSubmittedBy());
        response.setSubmittedAt(task.getSubmittedAt());
        response.setReviewedAt(task.getReviewedAt());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }

    private ReviewHistoryResponse toHistoryResponse(ReviewHistoryEntity history) {
        ReviewHistoryResponse response = new ReviewHistoryResponse();
        response.setId(history.getId());
        response.setTaskId(history.getTaskId());
        response.setAction(history.getAction());
        response.setFromStatus(history.getFromStatus());
        response.setFromStatusText(ReviewTaskStatus.fromCode(history.getFromStatus()).text());
        response.setToStatus(history.getToStatus());
        response.setToStatusText(ReviewTaskStatus.fromCode(history.getToStatus()).text());
        response.setOperatorId(history.getOperatorId());
        response.setComment(history.getComment());
        response.setCreatedAt(history.getCreatedAt());
        return response;
    }

    private String priorityText(Integer priority) {
        if (priority == null) {
            return "普通";
        }
        return switch (priority) {
            case 0 -> "低";
            case 2 -> "高";
            default -> "普通";
        };
    }
}
