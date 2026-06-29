package com.xcappstore.admin.software.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.service.OperationLogService;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.PackageAppendRequest;
import com.xcappstore.admin.software.dto.PackageScanRequest;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareUpdateRequest;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.dto.VersionCreateRequest;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationResult;
import com.xcappstore.admin.software.service.PackageFileStorageService.StoredPackage;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import com.xcappstore.admin.software.service.impl.SoftwareServiceImpl;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SoftwareServiceImplTest {
    private FakeSoftwareMapper softwareMapper;
    private FakeSoftwareCacheService softwareCacheService;
    private SoftwareServiceImpl softwareService;

    @BeforeEach
    void setUp() {
        softwareMapper = new FakeSoftwareMapper();
        softwareCacheService = new FakeSoftwareCacheService();
        softwareService = new SoftwareServiceImpl(
            softwareMapper,
            softwareCacheService,
            new PackageSecurityPolicyService(softwareMapper),
            new PackageScanPolicyService(),
            new PackagePreparationService(new FakePackageFileStorageService(), new FakePackageUploadSessionService()),
            new SoftwareAssembler(new ObjectMapper()),
            new FakeOperationLogService(),
            new ObjectMapper()
        );
    }

    @Test
    void returnsPagedSoftwareList() {
        softwareMapper.apps.put(1L, app(1L, "WPS Office", 2));

        SoftwareQueryRequest request = new SoftwareQueryRequest();
        request.setPage(1);
        request.setPageSize(10);
        PageResponse<SoftwareResponse> page = softwareService.page(request);

        assertEquals(1, page.getTotal());
        assertEquals("WPS Office", page.getList().get(0).getName());
        assertEquals("已上架", page.getList().get(0).getStatusText());
    }

    @Test
    void returnsCachedDetailWhenPresent() {
        SoftwareResponse cached = new SoftwareResponse();
        cached.setId(1L);
        cached.setName("缓存软件");
        softwareCacheService.cachedDetail = cached;

        SoftwareResponse response = softwareService.detail(1L);

        assertEquals("缓存软件", response.getName());
    }

    @Test
    void rejectsUnpublishForDraftSoftware() {
        softwareMapper.apps.put(1L, app(1L, "草稿软件", 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.unpublish(1L, 99L));

        assertEquals(ErrorCode.SOFTWARE_INVALID_STATUS, ex.getCode());
    }

    @Test
    void publishesApprovedUnpublishedSoftware() {
        softwareMapper.apps.put(1L, app(1L, "待上架软件", 3));

        SoftwareResponse response = softwareService.publish(1L, 99L);

        assertEquals(2, softwareMapper.apps.get(1L).getStatus());
        assertEquals(99L, softwareMapper.apps.get(1L).getUpdatedBy());
        assertEquals(0, softwareMapper.approvedVersionCount);
        assertEquals("已上架", response.getStatusText());
        assertEquals(1, softwareCacheService.invalidateCount);
    }

    @Test
    void rejectsPublishForDraftSoftware() {
        softwareMapper.apps.put(1L, app(1L, "草稿软件", 0));

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.publish(1L, 99L));

        assertEquals(ErrorCode.SOFTWARE_INVALID_STATUS, ex.getCode());
        assertEquals("软件需审核通过后才能上架", ex.getMessage());
    }

    @Test
    void rejectsPublishWhenPackageScanIsRisky() {
        softwareMapper.apps.put(1L, app(1L, "风险软件", 3));
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setId(10L);
        packageInfo.setAppId(1L);
        packageInfo.setVersionId(1L);
        packageInfo.setFileName("risky.deb");
        packageInfo.setScanStatus(2);
        softwareMapper.packages.put(10L, packageInfo);

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.publish(1L, 99L));

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("安装包未通过安全扫描，不能上架: risky.deb", ex.getMessage());
    }

    @Test
    void rejectsPublishWhenPackageScanIsMissing() {
        softwareMapper.apps.put(1L, app(1L, "未扫描软件", 3));
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setId(10L);
        packageInfo.setAppId(1L);
        packageInfo.setVersionId(1L);
        packageInfo.setFileName("unscanned.deb");
        packageInfo.setScanStatus(0);
        softwareMapper.packages.put(10L, packageInfo);

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.publish(1L, 99L));

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("安装包未通过安全扫描，不能上架: unscanned.deb", ex.getMessage());
    }

    @Test
    void uploadsSoftwareWithPackageMetadata() {
        softwareMapper.categoryCount = 1L;
        softwareMapper.tagCounts.put(2L, 1L);

        SoftwareUploadRequest request = new SoftwareUploadRequest();
        request.setAppKey("com.example.editor");
        request.setName("文本编辑器");
        request.setCategoryId(1L);
        request.setSummary("国产系统文本编辑器");
        request.setDescription("description");
        request.setVersionName("1.0.0");
        request.setVersionCode(100L);
        request.setOsType("uos_v20");
        request.setArch("x86_64");
        request.setPackageFormat("deb");
        request.setTagIds("2");
        request.setPackageFile(new MockMultipartFile("package_file", "editor.deb", "application/octet-stream", "deb".getBytes()));

        SoftwareResponse response = softwareService.upload(request, 99L);

        assertEquals("文本编辑器", response.getName());
        assertEquals(0, softwareMapper.apps.get(response.getId()).getStatus());
        assertEquals(1, softwareMapper.versions.size());
        assertEquals(1, softwareMapper.packages.size());
        assertEquals(1, softwareMapper.appTagCount);
        assertEquals("fake-sha256", softwareMapper.packages.values().iterator().next().getSha256());
    }

    @Test
    void rejectsUploadPublishNowBypassingReview() {
        softwareMapper.categoryCount = 1L;

        SoftwareUploadRequest request = new SoftwareUploadRequest();
        request.setAppKey("com.example.editor");
        request.setName("文本编辑器");
        request.setCategoryId(1L);
        request.setSummary("国产系统文本编辑器");
        request.setDescription("description");
        request.setVersionName("1.0.0");
        request.setVersionCode(100L);
        request.setOsType("uos_v20");
        request.setArch("x86_64");
        request.setPackageFormat("deb");
        request.setPublishNow(true);
        request.setPackageFile(new MockMultipartFile("package_file", "editor.deb", "application/octet-stream", "deb".getBytes()));

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.upload(request, 99L));

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("直接发布已关闭，请先提交审核并在审核通过后上架", ex.getMessage());
    }

    @Test
    void uploadsSoftwareFromCompletedUploadSession() {
        softwareMapper.categoryCount = 1L;

        SoftwareUploadRequest request = new SoftwareUploadRequest();
        request.setAppKey("com.example.chunked");
        request.setName("分片上传软件");
        request.setCategoryId(1L);
        request.setSummary("支持大文件续传");
        request.setDescription("description");
        request.setVersionName("1.0.0");
        request.setVersionCode(100L);
        request.setOsType("uos_v20");
        request.setArch("x86_64");
        request.setPackageFormat("deb");
        request.setUploadSessionId("upload-session-1");

        softwareService.upload(request, 99L);

        AppPackageEntity packageInfo = softwareMapper.packages.values().iterator().next();
        assertEquals("chunked-editor.deb", packageInfo.getFileName());
        assertEquals("chunked-sha256", packageInfo.getSha256());
        assertEquals(1, packageInfo.getSignatureStatus());
        assertEquals("sha256", packageInfo.getSignatureAlgorithm());
    }

    @Test
    void rejectsPackageFileAndUploadSessionTogether() {
        softwareMapper.categoryCount = 1L;

        SoftwareUploadRequest request = new SoftwareUploadRequest();
        request.setAppKey("com.example.conflict");
        request.setName("冲突软件");
        request.setCategoryId(1L);
        request.setSummary("summary");
        request.setDescription("description");
        request.setVersionName("1.0.0");
        request.setVersionCode(100L);
        request.setOsType("uos_v20");
        request.setArch("x86_64");
        request.setPackageFormat("deb");
        request.setUploadSessionId("upload-session-1");
        request.setPackageFile(new MockMultipartFile("package_file", "editor.deb", "application/octet-stream", "deb".getBytes()));

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.upload(request, 99L));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("安装包文件和上传会话不能同时提交", ex.getMessage());
    }

    @Test
    void updatesSoftwareMetadataAndTags() {
        softwareMapper.categoryCount = 1L;
        softwareMapper.tagCounts.put(2L, 1L);
        softwareMapper.apps.put(1L, app(1L, "旧名称", 0));

        SoftwareUpdateRequest request = new SoftwareUpdateRequest();
        request.setName("新名称");
        request.setCategoryId(1L);
        request.setSummary("新摘要");
        request.setDescription("新描述");
        request.setSupportedOsTypes("uos_v20,kylin_v10");
        request.setSupportedArchs("x86_64");
        request.setTagIds("2");
        request.setIsFeatured(1);
        request.setSortWeight(20);

        SoftwareResponse response = softwareService.update(1L, request, 99L);

        assertEquals("新名称", response.getName());
        assertEquals(99L, softwareMapper.apps.get(1L).getUpdatedBy());
        assertEquals(1, softwareMapper.deletedAppTagCount);
        assertEquals(1, softwareMapper.appTagCount);
        assertEquals(1, softwareCacheService.invalidateCount);
    }

    @Test
    void appendsDraftVersionAndPackage() {
        softwareMapper.apps.put(1L, app(1L, "版本软件", 3));

        VersionCreateRequest request = new VersionCreateRequest();
        request.setVersionName("2.0.0");
        request.setVersionCode(200L);
        request.setChangelog("新增版本");
        request.setOsType("uos_v20");
        request.setArch("x86_64");
        request.setPackageFormat("deb");
        request.setPackageFile(new MockMultipartFile("package_file", "editor-2.deb", "application/octet-stream", "deb".getBytes()));

        AppVersionResponse response = softwareService.addVersion(1L, request, 99L);

        assertEquals("2.0.0", response.getVersionName());
        assertEquals(0, response.getStatus());
        assertEquals(0, response.getIsLatest());
        assertEquals(1, response.getPackages().size());
        assertEquals(3, softwareMapper.apps.get(1L).getStatus());
        assertEquals(0, softwareMapper.markNotLatestCount);
    }

    @Test
    void rejectsAddVersionPublishNowBypassingReview() {
        softwareMapper.apps.put(1L, app(1L, "版本软件", 3));

        VersionCreateRequest request = new VersionCreateRequest();
        request.setVersionName("2.0.0");
        request.setVersionCode(200L);
        request.setOsType("uos_v20");
        request.setArch("x86_64");
        request.setPackageFormat("deb");
        request.setPublishNow(true);
        request.setPackageFile(new MockMultipartFile("package_file", "editor-2.deb", "application/octet-stream", "deb".getBytes()));

        BusinessException ex = assertThrows(BusinessException.class, () -> softwareService.addVersion(1L, request, 99L));

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("直接发布已关闭，请先提交审核并在审核通过后上架", ex.getMessage());
    }

    @Test
    void appendsPackageVariantToExistingVersion() {
        softwareMapper.apps.put(1L, app(1L, "多架构软件", 2));
        AppVersionEntity version = new AppVersionEntity();
        version.setId(10L);
        version.setAppId(1L);
        version.setVersionName("1.0.0");
        version.setVersionCode(100L);
        version.setStatus(2);
        version.setIsLatest(1);
        softwareMapper.versions.put(10L, version);

        PackageAppendRequest request = new PackageAppendRequest();
        request.setOsType("uos_v20");
        request.setArch("aarch64");
        request.setPackageFormat("deb");
        request.setPackageFile(new MockMultipartFile("package_file", "editor-arm.deb", "application/octet-stream", "deb".getBytes()));

        AppPackageResponse response = softwareService.addPackage(1L, 10L, request, 99L);

        assertEquals(10L, response.getVersionId());
        assertEquals("aarch64", response.getArch());
        assertEquals("可用", response.getStatusText());
        assertEquals(1, softwareMapper.packages.size());
    }

    @Test
    void scansPackageAsSafeAndAllowsPublish() {
        softwareMapper.apps.put(1L, app(1L, "扫描软件", 3));
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setId(10L);
        packageInfo.setAppId(1L);
        packageInfo.setVersionId(1L);
        packageInfo.setFileName("safe.deb");
        packageInfo.setScanStatus(0);
        softwareMapper.packages.put(10L, packageInfo);
        PackageScanRequest scanRequest = new PackageScanRequest();
        scanRequest.setResult("safe");

        AppPackageResponse scanResponse = softwareService.scanPackage(1L, 10L, scanRequest, 99L);
        SoftwareResponse publishResponse = softwareService.publish(1L, 99L);

        assertEquals(1, scanResponse.getScanStatus());
        assertEquals("安全", scanResponse.getScanStatusText());
        assertEquals("本地模拟扫描通过", softwareMapper.packages.get(10L).getScanReport());
        assertEquals("已上架", publishResponse.getStatusText());
    }

    private SoftwareEntity app(Long id, String name, Integer status) {
        SoftwareEntity app = new SoftwareEntity();
        app.setId(id);
        app.setAppKey("com.example." + id);
        app.setName(name);
        app.setDeveloperId(100L);
        app.setSubmitSource("admin");
        app.setCategoryId(1L);
        app.setCategoryName("办公软件");
        app.setIconUrl("https://example.com/icon.png");
        app.setSummary("summary");
        app.setDescription("description");
        app.setSupportedOsTypes("uos_v20,kylin_v10");
        app.setSupportedArchs("x86_64,aarch64");
        app.setScreenshots("[\"https://example.com/1.png\"]");
        app.setStatus(status);
        app.setIsOfficial(0);
        app.setIsFeatured(0);
        app.setSortWeight(0);
        app.setDownloadCount(0L);
        app.setRatingCount(0);
        app.setPackageCount(2L);
        app.setTagNames("办公,国产化");
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());
        return app;
    }

    private static final class FakeSoftwareMapper implements SoftwareMapper {
        private final Map<Long, SoftwareEntity> apps = new HashMap<>();
        private final Map<Long, AppVersionEntity> versions = new HashMap<>();
        private final Map<Long, AppPackageEntity> packages = new HashMap<>();
        private final Map<Long, Long> tagCounts = new HashMap<>();
        private long categoryCount;
        private long nextAppId = 1L;
        private long nextVersionId = 1L;
        private long nextPackageId = 1L;
        private int approvedVersionCount;
        private int appTagCount;
        private int deletedAppTagCount;
        private int markNotLatestCount;

        @Override
        public long countByAppKey(String appKey) {
            return apps.values().stream()
                .filter(app -> appKey.equals(app.getAppKey()))
                .count();
        }

        @Override
        public long countCategory(Long categoryId) {
            return categoryCount;
        }

        @Override
        public long countTag(Long tagId) {
            return tagCounts.getOrDefault(tagId, 0L);
        }

        @Override
        public int insertApp(SoftwareEntity app) {
            app.setId(nextAppId++);
            apps.put(app.getId(), app);
            return 1;
        }

        @Override
        public int insertVersion(AppVersionEntity version) {
            version.setId(nextVersionId++);
            versions.put(version.getId(), version);
            return 1;
        }

        @Override
        public int insertPackage(AppPackageEntity packageInfo) {
            packageInfo.setId(nextPackageId++);
            packages.put(packageInfo.getId(), packageInfo);
            return 1;
        }

        @Override
        public int insertAppTag(Long appId, Long tagId) {
            appTagCount++;
            return 1;
        }

        @Override
        public long countList(SoftwareQueryRequest query) {
            return apps.size();
        }

        @Override
        public List<SoftwareEntity> selectList(SoftwareQueryRequest query, int offset, int limit) {
            return apps.values().stream().toList();
        }

        @Override
        public SoftwareEntity selectById(Long id) {
            return apps.get(id);
        }

        @Override
        public int updateAppMetadata(SoftwareEntity app) {
            SoftwareEntity current = apps.get(app.getId());
            current.setName(app.getName());
            current.setCategoryId(app.getCategoryId());
            current.setIconUrl(app.getIconUrl());
            current.setSummary(app.getSummary());
            current.setDescription(app.getDescription());
            current.setSupportedOsTypes(app.getSupportedOsTypes());
            current.setSupportedArchs(app.getSupportedArchs());
            current.setScreenshots(app.getScreenshots());
            current.setIsOfficial(app.getIsOfficial());
            current.setIsFeatured(app.getIsFeatured());
            current.setSortWeight(app.getSortWeight());
            current.setUpdatedBy(app.getUpdatedBy());
            return 1;
        }

        @Override
        public int deleteAppTags(Long appId) {
            deletedAppTagCount++;
            return 1;
        }

        @Override
        public long countVersionCode(Long appId, Long versionCode) {
            return versions.values().stream()
                .filter(version -> appId.equals(version.getAppId()))
                .filter(version -> versionCode.equals(version.getVersionCode()))
                .count();
        }

        @Override
        public long countPackageVariant(Long versionId, String osType, String arch) {
            return packages.values().stream()
                .filter(packageInfo -> versionId.equals(packageInfo.getVersionId()))
                .filter(packageInfo -> osType.equals(packageInfo.getOsType()))
                .filter(packageInfo -> arch.equals(packageInfo.getArch()))
                .count();
        }

        @Override
        public int markVersionsNotLatest(Long appId, Long updatedBy) {
            versions.values().stream()
                .filter(version -> appId.equals(version.getAppId()))
                .forEach(version -> {
                    version.setIsLatest(0);
                    version.setUpdatedBy(updatedBy);
                });
            markNotLatestCount++;
            return 1;
        }

        @Override
        public AppVersionEntity selectVersionById(Long versionId) {
            return versions.get(versionId);
        }

        @Override
        public List<AppVersionEntity> selectVersionsByAppId(Long appId) {
            return versions.values().stream()
                .filter(version -> appId.equals(version.getAppId()))
                .toList();
        }

        @Override
        public List<AppPackageEntity> selectPackagesByAppId(Long appId) {
            return packages.values().stream()
                .filter(packageInfo -> appId.equals(packageInfo.getAppId()))
                .toList();
        }

        @Override
        public AppPackageEntity selectPackageById(Long packageId) {
            return packages.get(packageId);
        }

        @Override
        public int updatePackageScanResult(Long packageId, Integer scanStatus, String scanReport, Long updatedBy) {
            AppPackageEntity packageInfo = packages.get(packageId);
            if (packageInfo == null) {
                return 0;
            }
            packageInfo.setScanStatus(scanStatus);
            packageInfo.setScanReport(scanReport);
            packageInfo.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int updateStatus(Long id, Integer status, LocalDateTime publishedAt, Long updatedBy) {
            SoftwareEntity app = apps.get(id);
            app.setStatus(status);
            app.setPublishedAt(publishedAt);
            app.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int approveDraftVersions(Long appId, LocalDateTime reviewedAt, LocalDateTime publishedAt, Long updatedBy) {
            approvedVersionCount++;
            return 1;
        }

        @Override
        public int updateAppReviewing(Long id, Long updatedBy) {
            SoftwareEntity app = apps.get(id);
            app.setStatus(1);
            app.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int updateVersionReviewing(Long versionId, Long updatedBy) {
            AppVersionEntity version = versions.get(versionId);
            version.setStatus(1);
            version.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int updateDraftVersionsReviewing(Long appId, Long updatedBy) {
            versions.values().stream()
                .filter(version -> appId.equals(version.getAppId()))
                .forEach(version -> {
                    version.setStatus(1);
                    version.setUpdatedBy(updatedBy);
                });
            return 1;
        }

        @Override
        public int approveVersion(Long versionId, LocalDateTime reviewedAt, LocalDateTime publishedAt, Long updatedBy) {
            AppVersionEntity version = versions.get(versionId);
            version.setStatus(2);
            version.setIsLatest(1);
            version.setReviewedAt(reviewedAt);
            version.setPublishedAt(publishedAt);
            version.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int rejectAppReview(Long id, LocalDateTime rejectedAt, Long updatedBy) {
            SoftwareEntity app = apps.get(id);
            app.setStatus(4);
            app.setRejectedAt(rejectedAt);
            app.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int rejectVersion(Long versionId, LocalDateTime reviewedAt, Long updatedBy) {
            AppVersionEntity version = versions.get(versionId);
            version.setStatus(3);
            version.setReviewedAt(reviewedAt);
            version.setUpdatedBy(updatedBy);
            return 1;
        }

        @Override
        public int rejectDraftVersions(Long appId, LocalDateTime reviewedAt, Long updatedBy) {
            versions.values().stream()
                .filter(version -> appId.equals(version.getAppId()))
                .forEach(version -> {
                    version.setStatus(3);
                    version.setReviewedAt(reviewedAt);
                    version.setUpdatedBy(updatedBy);
                });
            return 1;
        }
    }

    private static final class FakeOperationLogService implements OperationLogService {
        @Override
        public com.xcappstore.admin.common.PageResponse<com.xcappstore.admin.operationlog.dto.OperationLogResponse> list(com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest request) {
            return new com.xcappstore.admin.common.PageResponse<>(0, 1, 20, List.of());
        }

        @Override
        public com.xcappstore.admin.operationlog.dto.OperationLogResponse detail(Long id) {
            return null;
        }

        @Override
        public com.xcappstore.admin.operationlog.dto.OperationLogOptionsResponse options() {
            return new com.xcappstore.admin.operationlog.dto.OperationLogOptionsResponse(List.of(), List.of());
        }

        @Override
        public com.xcappstore.admin.operationlog.dto.OperationLogStatsResponse stats(com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest request) {
            return new com.xcappstore.admin.operationlog.dto.OperationLogStatsResponse(List.of(), List.of(), List.of());
        }

        @Override
        public void record(OperationLogCreateCommand command) {
        }
    }

    private static final class FakePackageFileStorageService extends PackageFileStorageService {
        private FakePackageFileStorageService() {
            super("/tmp/xcappstore-admin-test-packages", "500MB");
        }

        @Override
        public StoredPackage store(org.springframework.web.multipart.MultipartFile file, String packageFormat) {
            return new StoredPackage(file.getOriginalFilename(), file.getSize(), "fake/editor.deb", "fake-sha256");
        }
    }

    private static final class FakePackageUploadSessionService extends PackageUploadSessionService {
        private FakePackageUploadSessionService() {
            super(null, null, null);
        }

        @Override
        public VerifiedPackage consumeCompletedSession(String uploadId, String packageFormat, Long adminUserId) {
            return new VerifiedPackage(
                new StoredPackage("chunked-editor.deb", 12L, "fake/chunked-editor.deb", "chunked-sha256"),
                PackageVerificationResult.verified("sha256", "chunked-sha256")
            );
        }
    }

    private static final class FakeSoftwareCacheService extends SoftwareCacheService {
        private SoftwareResponse cachedDetail;
        private int invalidateCount;

        private FakeSoftwareCacheService() {
            super(null, null);
        }

        @Override
        public Optional<SoftwareResponse> getDetail(Long id) {
            return Optional.ofNullable(cachedDetail);
        }

        @Override
        public void putDetail(Long id, SoftwareResponse response) {
            cachedDetail = response;
        }

        @Override
        public void invalidateDetail(Long id) {
            cachedDetail = null;
            invalidateCount++;
        }
    }
}
