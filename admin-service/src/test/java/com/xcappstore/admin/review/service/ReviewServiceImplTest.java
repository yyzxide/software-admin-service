package com.xcappstore.admin.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.service.OperationLogService;
import com.xcappstore.admin.review.dto.ReviewDecisionRequest;
import com.xcappstore.admin.review.dto.ReviewTaskCreateRequest;
import com.xcappstore.admin.review.dto.ReviewTaskResponse;
import com.xcappstore.admin.review.entity.ReviewHistoryEntity;
import com.xcappstore.admin.review.entity.ReviewTaskEntity;
import com.xcappstore.admin.review.mapper.ReviewMapper;
import com.xcappstore.admin.review.service.impl.ReviewServiceImpl;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import com.xcappstore.admin.software.service.PackageSecurityPolicyService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReviewServiceImplTest {
    private FakeReviewMapper reviewMapper;
    private FakeSoftwareMapper softwareMapper;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewMapper = new FakeReviewMapper();
        softwareMapper = new FakeSoftwareMapper();
        reviewService = new ReviewServiceImpl(
            reviewMapper,
            softwareMapper,
            new PackageSecurityPolicyService(softwareMapper),
            new FakeOperationLogService()
        );
    }

    @Test
    void createsReviewTaskAndMarksSoftwareReviewing() {
        softwareMapper.apps.put(1L, app(1L, "文本编辑器", 0));
        ReviewTaskCreateRequest request = new ReviewTaskCreateRequest();
        request.setAppId(1L);
        request.setReason("准备上架");

        ReviewTaskResponse response = reviewService.create(request, 99L);

        assertEquals("文本编辑器 上架审核", response.getTitle());
        assertEquals(1, softwareMapper.apps.get(1L).getStatus());
        assertEquals(1, reviewMapper.histories.size());
    }

    @Test
    void approvesReviewTaskAndPublishesSoftware() {
        softwareMapper.apps.put(1L, app(1L, "文本编辑器", 1));
        reviewMapper.tasks.put(1L, task(1L, 1L, null, 1));
        ReviewDecisionRequest request = new ReviewDecisionRequest();
        request.setComment("通过");

        ReviewTaskResponse response = reviewService.approve(1L, request, 88L);

        assertEquals("已通过", response.getStatusText());
        assertEquals(2, softwareMapper.apps.get(1L).getStatus());
        assertEquals(1, softwareMapper.approvedDraftVersions);
    }

    @Test
    void rejectsApproveWhenPackageSignatureFailed() {
        softwareMapper.apps.put(1L, app(1L, "文本编辑器", 1));
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setId(10L);
        packageInfo.setAppId(1L);
        packageInfo.setVersionId(1L);
        packageInfo.setFileName("bad-signature.deb");
        packageInfo.setSignatureStatus(2);
        softwareMapper.packages.put(10L, packageInfo);
        reviewMapper.tasks.put(1L, task(1L, 1L, null, 1));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> reviewService.approve(1L, new ReviewDecisionRequest(), 88L)
        );

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("安装包签名校验失败，不能上架: bad-signature.deb", ex.getMessage());
    }

    @Test
    void rejectsFinishedReviewTask() {
        softwareMapper.apps.put(1L, app(1L, "文本编辑器", 2));
        reviewMapper.tasks.put(1L, task(1L, 1L, null, 2));

        BusinessException ex = assertThrows(BusinessException.class, () -> reviewService.reject(1L, new ReviewDecisionRequest(), 88L));

        assertEquals(ErrorCode.REVIEW_INVALID_STATUS, ex.getCode());
    }

    @Test
    void rejectsConcurrentReviewDecisionWhenConditionalUpdateAffectsNoRows() {
        softwareMapper.apps.put(1L, app(1L, "文本编辑器", 1));
        reviewMapper.tasks.put(1L, task(1L, 1L, null, 1));
        reviewMapper.finishAffectedRows = 0;

        BusinessException ex = assertThrows(BusinessException.class, () -> reviewService.approve(1L, new ReviewDecisionRequest(), 88L));

        assertEquals(ErrorCode.REVIEW_INVALID_STATUS, ex.getCode());
        assertEquals("审核任务已被其他人处理", ex.getMessage());
        assertEquals(0, reviewMapper.histories.size());
        assertEquals(1, softwareMapper.apps.get(1L).getStatus());
    }

    @Test
    void rejectsApproveWhenPackageNotScanned() {
        softwareMapper.apps.put(1L, app(1L, "文本编辑器", 1));
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setId(10L);
        packageInfo.setAppId(1L);
        packageInfo.setVersionId(1L);
        packageInfo.setFileName("unscanned.deb");
        packageInfo.setScanStatus(0);
        softwareMapper.packages.put(10L, packageInfo);
        reviewMapper.tasks.put(1L, task(1L, 1L, null, 1));

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> reviewService.approve(1L, new ReviewDecisionRequest(), 88L)
        );

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("安装包未通过安全扫描，不能上架: unscanned.deb", ex.getMessage());
    }

    private SoftwareEntity app(Long id, String name, Integer status) {
        SoftwareEntity app = new SoftwareEntity();
        app.setId(id);
        app.setName(name);
        app.setStatus(status);
        app.setAppKey("com.example." + id);
        return app;
    }

    private ReviewTaskEntity task(Long id, Long appId, Long versionId, Integer status) {
        ReviewTaskEntity task = new ReviewTaskEntity();
        task.setId(id);
        task.setAppId(appId);
        task.setVersionId(versionId);
        task.setTargetType(versionId == null ? "software" : "version");
        task.setTitle("文本编辑器 上架审核");
        task.setStatus(status);
        task.setPriority(1);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private static final class FakeReviewMapper implements ReviewMapper {
        private final Map<Long, ReviewTaskEntity> tasks = new HashMap<>();
        private final List<ReviewHistoryEntity> histories = new ArrayList<>();
        private long nextTaskId = 1L;
        private long nextHistoryId = 1L;
        private int finishAffectedRows = 1;

        @Override public int insertTask(ReviewTaskEntity task) { task.setId(nextTaskId++); tasks.put(task.getId(), task); return 1; }
        @Override public int insertHistory(ReviewHistoryEntity history) { history.setId(nextHistoryId++); histories.add(history); return 1; }
        @Override public long countActiveTask(Long appId, Long versionId) { return 0; }
        @Override public long count(com.xcappstore.admin.review.dto.ReviewTaskQueryRequest query) { return tasks.size(); }
        @Override public List<ReviewTaskEntity> selectPage(com.xcappstore.admin.review.dto.ReviewTaskQueryRequest query, int offset, int limit) { return tasks.values().stream().toList(); }
        @Override public ReviewTaskEntity selectById(Long id) { return tasks.get(id); }
        @Override public List<ReviewHistoryEntity> selectHistories(Long taskId) { return histories.stream().filter(h -> taskId.equals(h.getTaskId())).toList(); }
        @Override public int assign(Long id, Long reviewerId, Integer status, LocalDateTime updatedAt) { ReviewTaskEntity task = tasks.get(id); if (task.getStatus() != 0 && task.getStatus() != 1) { return 0; } task.setReviewerId(reviewerId); task.setStatus(status); task.setUpdatedAt(updatedAt); return 1; }
        @Override public int finish(Long id, Integer status, String reviewComment, Long reviewerId, LocalDateTime reviewedAt, LocalDateTime updatedAt) { if (finishAffectedRows == 0) { return 0; } ReviewTaskEntity task = tasks.get(id); if (task.getStatus() != 0 && task.getStatus() != 1) { return 0; } task.setStatus(status); task.setReviewComment(reviewComment); task.setReviewerId(reviewerId); task.setReviewedAt(reviewedAt); task.setUpdatedAt(updatedAt); return 1; }
    }

    private static final class FakeSoftwareMapper implements SoftwareMapper {
        private final Map<Long, SoftwareEntity> apps = new HashMap<>();
        private final Map<Long, AppVersionEntity> versions = new HashMap<>();
        private final Map<Long, AppPackageEntity> packages = new HashMap<>();
        private int approvedDraftVersions;

        @Override public long countByAppKey(String appKey) { return 0; }
        @Override public long countCategory(Long categoryId) { return 1; }
        @Override public long countTag(Long tagId) { return 1; }
        @Override public int insertApp(SoftwareEntity app) { return 1; }
        @Override public int insertVersion(AppVersionEntity version) { return 1; }
        @Override public int insertPackage(AppPackageEntity packageInfo) { return 1; }
        @Override public int insertAppTag(Long appId, Long tagId) { return 1; }
        @Override public long countList(SoftwareQueryRequest query) { return 0; }
        @Override public List<SoftwareEntity> selectList(SoftwareQueryRequest query, int offset, int limit) { return List.of(); }
        @Override public SoftwareEntity selectById(Long id) { return apps.get(id); }
        @Override public int updateAppMetadata(SoftwareEntity app) { return 1; }
        @Override public int deleteAppTags(Long appId) { return 1; }
        @Override public long countVersionCode(Long appId, Long versionCode) { return 0; }
        @Override public long countPackageVariant(Long versionId, String osType, String arch) { return 0; }
        @Override public int markVersionsNotLatest(Long appId, Long updatedBy) { return 1; }
        @Override public AppVersionEntity selectVersionById(Long versionId) { return versions.get(versionId); }
        @Override public List<AppVersionEntity> selectVersionsByAppId(Long appId) { return List.of(); }
        @Override public List<AppPackageEntity> selectPackagesByAppId(Long appId) { return packages.values().stream().filter(packageInfo -> appId.equals(packageInfo.getAppId())).toList(); }
        @Override public AppPackageEntity selectPackageById(Long packageId) { return packages.get(packageId); }
        @Override public int updatePackageScanResult(Long packageId, Integer scanStatus, String scanReport, Long updatedBy) { return 1; }
        @Override public int updateStatus(Long id, Integer status, LocalDateTime publishedAt, Long updatedBy) { SoftwareEntity app = apps.get(id); app.setStatus(status); app.setPublishedAt(publishedAt); app.setUpdatedBy(updatedBy); return 1; }
        @Override public int approveDraftVersions(Long appId, LocalDateTime reviewedAt, LocalDateTime publishedAt, Long updatedBy) { approvedDraftVersions++; return 1; }
        @Override public int updateAppReviewing(Long id, Long updatedBy) { apps.get(id).setStatus(1); return 1; }
        @Override public int updateVersionReviewing(Long versionId, Long updatedBy) { return 1; }
        @Override public int updateDraftVersionsReviewing(Long appId, Long updatedBy) { return 1; }
        @Override public int approveVersion(Long versionId, LocalDateTime reviewedAt, LocalDateTime publishedAt, Long updatedBy) { return 1; }
        @Override public int rejectAppReview(Long id, LocalDateTime rejectedAt, Long updatedBy) { apps.get(id).setStatus(4); return 1; }
        @Override public int rejectVersion(Long versionId, LocalDateTime reviewedAt, Long updatedBy) { return 1; }
        @Override public int rejectDraftVersions(Long appId, LocalDateTime reviewedAt, Long updatedBy) { return 1; }
    }

    private static final class FakeOperationLogService implements OperationLogService {
        @Override public com.xcappstore.admin.common.PageResponse<com.xcappstore.admin.operationlog.dto.OperationLogResponse> list(com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest request) { return new PageResponse<>(0, 1, 20, List.of()); }
        @Override public com.xcappstore.admin.operationlog.dto.OperationLogResponse detail(Long id) { return null; }
        @Override public com.xcappstore.admin.operationlog.dto.OperationLogOptionsResponse options() { return new com.xcappstore.admin.operationlog.dto.OperationLogOptionsResponse(List.of(), List.of()); }
        @Override public com.xcappstore.admin.operationlog.dto.OperationLogStatsResponse stats(com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest request) { return new com.xcappstore.admin.operationlog.dto.OperationLogStatsResponse(List.of(), List.of(), List.of()); }
        @Override public void record(OperationLogCreateCommand command) { }
    }
}
