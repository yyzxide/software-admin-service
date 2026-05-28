package com.xcappstore.admin.software.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
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
            new FakePackageFileStorageService(),
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
    void publishesSoftwareAndApprovesDraftVersions() {
        softwareMapper.apps.put(1L, app(1L, "待上架软件", 1));

        SoftwareResponse response = softwareService.publish(1L, 99L);

        assertEquals(2, softwareMapper.apps.get(1L).getStatus());
        assertEquals(99L, softwareMapper.apps.get(1L).getUpdatedBy());
        assertEquals(1, softwareMapper.approvedVersionCount);
        assertEquals("已上架", response.getStatusText());
        assertEquals(1, softwareCacheService.invalidateCount);
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
        request.setPublishNow(true);
        request.setPackageFile(new MockMultipartFile("package_file", "editor.deb", "application/octet-stream", "deb".getBytes()));

        SoftwareResponse response = softwareService.upload(request, 99L);

        assertEquals("文本编辑器", response.getName());
        assertEquals(2, softwareMapper.apps.get(response.getId()).getStatus());
        assertEquals(1, softwareMapper.versions.size());
        assertEquals(1, softwareMapper.packages.size());
        assertEquals(1, softwareMapper.appTagCount);
        assertEquals("fake-sha256", softwareMapper.packages.values().iterator().next().getSha256());
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
    }

    private static final class FakePackageFileStorageService extends PackageFileStorageService {
        private FakePackageFileStorageService() {
            super("/tmp/xcappstore-admin-test-packages");
        }

        @Override
        public StoredPackage store(org.springframework.web.multipart.MultipartFile file) {
            return new StoredPackage(file.getOriginalFilename(), file.getSize(), "fake/editor.deb", "fake-sha256");
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
