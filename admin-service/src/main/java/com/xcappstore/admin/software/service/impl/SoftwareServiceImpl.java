package com.xcappstore.admin.software.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.service.OperationLogPublisher;
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
import com.xcappstore.admin.software.model.AppVersionStatus;
import com.xcappstore.admin.software.model.SoftwareStatus;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import com.xcappstore.admin.software.service.PackagePreparationService;
import com.xcappstore.admin.software.service.PackagePreparationService.PackageSpec;
import com.xcappstore.admin.software.service.PackageScanPolicyService;
import com.xcappstore.admin.software.service.PackageSecurityPolicyService;
import com.xcappstore.admin.software.service.SoftwareAssembler;
import com.xcappstore.admin.software.service.SoftwareCacheService;
import com.xcappstore.admin.software.service.SoftwareService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SoftwareServiceImpl implements SoftwareService {
    private static final String SUBMIT_SOURCE_ADMIN = "admin";

    private final SoftwareMapper softwareMapper;
    private final SoftwareCacheService softwareCacheService;
    private final PackageSecurityPolicyService packageSecurityPolicyService;
    private final PackageScanPolicyService packageScanPolicyService;
    private final PackagePreparationService packagePreparationService;
    private final SoftwareAssembler softwareAssembler;
    private final OperationLogPublisher operationLogPublisher;
    private final ObjectMapper objectMapper;

    public SoftwareServiceImpl(
        SoftwareMapper softwareMapper,
        SoftwareCacheService softwareCacheService,
        PackageSecurityPolicyService packageSecurityPolicyService,
        PackageScanPolicyService packageScanPolicyService,
        PackagePreparationService packagePreparationService,
        SoftwareAssembler softwareAssembler,
        OperationLogPublisher operationLogPublisher,
        ObjectMapper objectMapper
    ) {
        this.softwareMapper = softwareMapper;
        this.softwareCacheService = softwareCacheService;
        this.packageSecurityPolicyService = packageSecurityPolicyService;
        this.packageScanPolicyService = packageScanPolicyService;
        this.packagePreparationService = packagePreparationService;
        this.softwareAssembler = softwareAssembler;
        this.operationLogPublisher = operationLogPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public SoftwareResponse upload(SoftwareUploadRequest request, Long adminUserId) {
        Long operatorId = normalizeAdminUserId(adminUserId);
        String appKey = normalizeText(request.getAppKey());
        if (softwareMapper.countByAppKey(appKey) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "软件标识已存在");
        }
        if (softwareMapper.countCategory(request.getCategoryId()) == 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分类不存在或已禁用");
        }

        PackageSpec packageSpec = packagePreparationService.requireSpec(
            request.getOsType(),
            request.getArch(),
            request.getPackageFormat()
        );
        List<Long> tagIds = parseTagIds(request.getTagIds());
        validateTags(tagIds);
        rejectDirectPublish(Boolean.TRUE.equals(request.getPublishNow()));

        VerifiedPackage verifiedPackage = packagePreparationService.resolvePackage(
            request.getPackageFile(),
            request.getUploadSessionId(),
            packageSpec.packageFormat(),
            request.getExpectedSha256(),
            request.getSignatureAlgorithm(),
            request.getSignatureValue(),
            operatorId
        );
        boolean cleanupOnFailure = isDirectPackageUpload(request.getPackageFile(), request.getUploadSessionId());
        try {
            LocalDateTime now = LocalDateTime.now();
            SoftwareEntity app = new SoftwareEntity();
            app.setAppKey(appKey);
            app.setName(normalizeText(request.getName()));
            app.setDeveloperId(operatorId);
            app.setSubmitSource(SUBMIT_SOURCE_ADMIN);
            app.setCategoryId(request.getCategoryId());
            app.setIconUrl(defaultText(normalizeText(request.getIconUrl()), ""));
            app.setSummary(normalizeText(request.getSummary()));
            app.setDescription(normalizeText(request.getDescription()));
            app.setSupportedOsTypes(defaultText(normalizeCsv(request.getSupportedOsTypes()), packageSpec.osType()));
            app.setSupportedArchs(defaultText(normalizeCsv(request.getSupportedArchs()), packageSpec.arch()));
            app.setScreenshots(normalizeScreenshots(request.getScreenshots()));
            app.setStatus(SoftwareStatus.DRAFT.code());
            app.setIsOfficial(0);
            app.setIsFeatured(0);
            app.setSortWeight(0);
            app.setDownloadCount(0L);
            app.setRatingScore(BigDecimal.ZERO);
            app.setRatingCount(0);
            app.setPublishedAt(null);
            app.setCreatedAt(now);
            app.setUpdatedAt(now);
            app.setCreatedBy(operatorId);
            app.setUpdatedBy(operatorId);
            softwareMapper.insertApp(app);

            AppVersionEntity version = new AppVersionEntity();
            version.setAppId(app.getId());
            version.setVersionName(normalizeText(request.getVersionName()));
            version.setVersionCode(request.getVersionCode() == null ? System.currentTimeMillis() / 1000 : request.getVersionCode());
            version.setSubmitSource(SUBMIT_SOURCE_ADMIN);
            version.setStatus(AppVersionStatus.DRAFT.code());
            version.setIsLatest(1);
            version.setSubmittedAt(now);
            version.setReviewedAt(null);
            version.setPublishedAt(null);
            version.setCreatedAt(now);
            version.setUpdatedAt(now);
            version.setCreatedBy(operatorId);
            version.setUpdatedBy(operatorId);
            softwareMapper.insertVersion(version);

            AppPackageEntity packageInfo = packagePreparationService.buildAvailablePackage(
                app.getId(),
                version.getId(),
                packageSpec,
                verifiedPackage,
                operatorId,
                now
            );
            softwareMapper.insertPackage(packageInfo);

            for (Long tagId : tagIds) {
                softwareMapper.insertAppTag(app.getId(), tagId);
            }

            softwareCacheService.invalidateDetail(app.getId());
            SoftwareResponse response = freshDetail(app.getId());
            operationLogPublisher.record(operatorId, "software_upload", "software", app.getId(), app.getName(), "上传软件: " + app.getName());
            return response;
        } catch (RuntimeException ex) {
            cleanupDirectPackageIfNecessary(cleanupOnFailure, verifiedPackage);
            throw ex;
        }
    }

    @Override
    public PageResponse<SoftwareResponse> page(SoftwareQueryRequest request) {
        normalizeQuery(request);
        long total = softwareMapper.countList(request);
        List<SoftwareResponse> list = total == 0 ? List.of() : softwareMapper
            .selectList(request, request.offset(), request.getPageSize())
            .stream()
            .map(softwareAssembler::toResponse)
            .toList();
        return new PageResponse<>(total, request.getPage(), request.getPageSize(), list);
    }

    @Override
    public SoftwareResponse detail(Long id) {
        requireValidId(id);
        return softwareCacheService.getDetail(id).orElseGet(() -> {
            SoftwareResponse response = softwareAssembler.toResponse(requireSoftware(id));
            softwareCacheService.putDetail(id, response);
            return response;
        });
    }

    @Override
    @Transactional
    public SoftwareResponse update(Long id, SoftwareUpdateRequest request, Long adminUserId) {
        SoftwareEntity current = requireSoftware(id);
        Long operatorId = normalizeAdminUserId(adminUserId);
        if (softwareMapper.countCategory(request.getCategoryId()) == 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分类不存在或已禁用");
        }

        List<Long> tagIds = parseTagIds(request.getTagIds());
        validateTags(tagIds);

        SoftwareEntity updated = new SoftwareEntity();
        updated.setId(current.getId());
        updated.setName(normalizeText(request.getName()));
        updated.setCategoryId(request.getCategoryId());
        updated.setIconUrl(defaultText(normalizeText(request.getIconUrl()), ""));
        updated.setSummary(normalizeText(request.getSummary()));
        updated.setDescription(normalizeText(request.getDescription()));
        updated.setSupportedOsTypes(defaultText(normalizeCsv(request.getSupportedOsTypes()), current.getSupportedOsTypes()));
        updated.setSupportedArchs(defaultText(normalizeCsv(request.getSupportedArchs()), current.getSupportedArchs()));
        updated.setScreenshots(
            StringUtils.hasText(request.getScreenshots())
                ? normalizeScreenshots(request.getScreenshots())
                : current.getScreenshots()
        );
        updated.setIsOfficial(normalizeFlag(request.getIsOfficial(), current.getIsOfficial(), "官方标记只能是0或1"));
        updated.setIsFeatured(normalizeFlag(request.getIsFeatured(), current.getIsFeatured(), "推荐标记只能是0或1"));
        updated.setSortWeight(request.getSortWeight() == null ? current.getSortWeight() : request.getSortWeight());
        updated.setUpdatedBy(operatorId);

        softwareMapper.updateAppMetadata(updated);
        softwareMapper.deleteAppTags(id);
        for (Long tagId : tagIds) {
            softwareMapper.insertAppTag(id, tagId);
        }

        softwareCacheService.invalidateDetail(id);
        SoftwareResponse response = freshDetail(id);
        operationLogPublisher.record(operatorId, "software_update", "software", id, response.getName(), "编辑软件资料: " + response.getName());
        return response;
    }

    @Override
    @Transactional
    public SoftwareResponse publish(Long id, Long adminUserId) {
        SoftwareEntity app = requireSoftware(id);
        SoftwareStatus status = SoftwareStatus.fromCode(app.getStatus());
        if (status == SoftwareStatus.PUBLISHED) {
            return freshDetail(id);
        }
        if (!status.canPublish()) {
            throw new BusinessException(ErrorCode.SOFTWARE_INVALID_STATUS, "软件需审核通过后才能上架");
        }

        Long operatorId = normalizeAdminUserId(adminUserId);
        packageSecurityPolicyService.assertAppPackagesPublishable(id);
        LocalDateTime now = LocalDateTime.now();
        softwareMapper.updateStatus(id, SoftwareStatus.PUBLISHED.code(), now, operatorId);
        softwareCacheService.invalidateDetail(id);
        SoftwareResponse response = freshDetail(id);
        operationLogPublisher.record(operatorId, "software_publish", "software", id, response.getName(), "上架软件: " + response.getName());
        return response;
    }

    @Override
    @Transactional
    public SoftwareResponse unpublish(Long id, Long adminUserId) {
        SoftwareEntity app = requireSoftware(id);
        SoftwareStatus status = SoftwareStatus.fromCode(app.getStatus());
        if (status == SoftwareStatus.UNPUBLISHED) {
            return freshDetail(id);
        }
        if (!status.canUnpublish()) {
            throw new BusinessException(ErrorCode.SOFTWARE_INVALID_STATUS, "当前状态不允许下架");
        }

        Long operatorId = normalizeAdminUserId(adminUserId);
        softwareMapper.updateStatus(id, SoftwareStatus.UNPUBLISHED.code(), app.getPublishedAt(), operatorId);
        softwareCacheService.invalidateDetail(id);
        SoftwareResponse response = freshDetail(id);
        operationLogPublisher.record(operatorId, "software_unpublish", "software", id, response.getName(), "下架软件: " + response.getName());
        return response;
    }

    @Override
    @Transactional
    public AppVersionResponse addVersion(Long appId, VersionCreateRequest request, Long adminUserId) {
        SoftwareEntity app = requireSoftware(appId);
        Long operatorId = normalizeAdminUserId(adminUserId);
        PackageSpec packageSpec = packagePreparationService.requireSpec(
            request.getOsType(),
            request.getArch(),
            request.getPackageFormat()
        );
        Long versionCode = request.getVersionCode() == null ? System.currentTimeMillis() / 1000 : request.getVersionCode();
        if (versionCode <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "版本号必须大于0");
        }
        if (softwareMapper.countVersionCode(appId, versionCode) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "版本号已存在");
        }

        rejectDirectPublish(Boolean.TRUE.equals(request.getPublishNow()));
        VerifiedPackage verifiedPackage = packagePreparationService.resolvePackage(
            request.getPackageFile(),
            request.getUploadSessionId(),
            packageSpec.packageFormat(),
            request.getExpectedSha256(),
            request.getSignatureAlgorithm(),
            request.getSignatureValue(),
            operatorId
        );
        boolean cleanupOnFailure = isDirectPackageUpload(request.getPackageFile(), request.getUploadSessionId());
        try {
            LocalDateTime now = LocalDateTime.now();
            AppVersionEntity version = new AppVersionEntity();
            version.setAppId(appId);
            version.setVersionName(normalizeText(request.getVersionName()));
            version.setVersionCode(versionCode);
            version.setChangelog(normalizeText(request.getChangelog()));
            version.setSubmitSource(SUBMIT_SOURCE_ADMIN);
            version.setStatus(AppVersionStatus.DRAFT.code());
            version.setIsLatest(0);
            version.setSubmittedAt(now);
            version.setReviewedAt(null);
            version.setPublishedAt(null);
            version.setCreatedAt(now);
            version.setUpdatedAt(now);
            version.setCreatedBy(operatorId);
            version.setUpdatedBy(operatorId);
            softwareMapper.insertVersion(version);

            AppPackageEntity packageInfo = packagePreparationService.buildAvailablePackage(
                appId,
                version.getId(),
                packageSpec,
                verifiedPackage,
                operatorId,
                now
            );
            softwareMapper.insertPackage(packageInfo);

            softwareCacheService.invalidateDetail(appId);
            operationLogPublisher.record(operatorId, "software_version_create", "software_version", version.getId(), app.getName() + " " + version.getVersionName(), "新增软件版本: " + app.getName() + " " + version.getVersionName());
            AppVersionResponse response = softwareAssembler.toVersionResponse(version);
            response.setPackages(List.of(softwareAssembler.toPackageResponse(packageInfo)));
            return response;
        } catch (RuntimeException ex) {
            cleanupDirectPackageIfNecessary(cleanupOnFailure, verifiedPackage);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AppPackageResponse addPackage(Long appId, Long versionId, PackageAppendRequest request, Long adminUserId) {
        requireSoftware(appId);
        requireVersion(appId, versionId);
        Long operatorId = normalizeAdminUserId(adminUserId);
        PackageSpec packageSpec = packagePreparationService.requireSpec(
            request.getOsType(),
            request.getArch(),
            request.getPackageFormat()
        );
        if (softwareMapper.countPackageVariant(versionId, packageSpec.osType(), packageSpec.arch(), packageSpec.packageFormat()) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该版本已存在相同系统和架构的安装包");
        }

        VerifiedPackage verifiedPackage = packagePreparationService.resolvePackage(
            request.getPackageFile(),
            request.getUploadSessionId(),
            packageSpec.packageFormat(),
            request.getExpectedSha256(),
            request.getSignatureAlgorithm(),
            request.getSignatureValue(),
            operatorId
        );
        boolean cleanupOnFailure = isDirectPackageUpload(request.getPackageFile(), request.getUploadSessionId());
        try {
            LocalDateTime now = LocalDateTime.now();
            AppPackageEntity packageInfo = packagePreparationService.buildAvailablePackage(
                appId,
                versionId,
                packageSpec,
                verifiedPackage,
                operatorId,
                now
            );
            softwareMapper.insertPackage(packageInfo);

            softwareCacheService.invalidateDetail(appId);
            operationLogPublisher.record(operatorId, "software_package_create", "software_package", packageInfo.getId(), packageInfo.getFileName(), "新增安装包: " + packageInfo.getFileName());
            return softwareAssembler.toPackageResponse(packageInfo);
        } catch (RuntimeException ex) {
            cleanupDirectPackageIfNecessary(cleanupOnFailure, verifiedPackage);
            throw ex;
        }
    }

    @Override
    @Transactional
    public AppPackageResponse scanPackage(Long appId, Long packageId, PackageScanRequest request, Long adminUserId) {
        requireSoftware(appId);
        AppPackageEntity packageInfo = requirePackage(appId, packageId);
        Long operatorId = normalizeAdminUserId(adminUserId);
        Integer scanStatus = packageScanPolicyService.parseScanResult(request == null ? null : request.getResult());
        String scanReport = defaultText(
            normalizeText(request == null ? null : request.getReport()),
            packageScanPolicyService.defaultScanReport(scanStatus)
        );
        int affected = softwareMapper.updatePackageScanResult(packageId, scanStatus, scanReport, operatorId);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "安装包不存在");
        }
        packageInfo.setScanStatus(scanStatus);
        packageInfo.setScanReport(scanReport);
        packageInfo.setUpdatedBy(operatorId);
        packageInfo.setUpdatedAt(LocalDateTime.now());
        softwareCacheService.invalidateDetail(appId);
        operationLogPublisher.record(operatorId, "software_package_scan", "software_package", packageId, packageInfo.getFileName(), "安装包安全状态更新: " + packageInfo.getFileName());
        return softwareAssembler.toPackageResponse(packageInfo);
    }

    @Override
    public List<AppVersionResponse> versions(Long appId) {
        requireSoftware(appId);
        Map<Long, List<AppPackageResponse>> packagesByVersion = softwareMapper.selectPackagesByAppId(appId)
            .stream()
            .map(softwareAssembler::toPackageResponse)
            .collect(Collectors.groupingBy(AppPackageResponse::getVersionId));
        return softwareMapper.selectVersionsByAppId(appId)
            .stream()
            .map(version -> {
                AppVersionResponse response = softwareAssembler.toVersionResponse(version);
                response.setPackages(packagesByVersion.getOrDefault(version.getId(), List.of()));
                return response;
            })
            .toList();
    }

    @Override
    public List<AppPackageResponse> packages(Long appId) {
        requireSoftware(appId);
        return softwareMapper.selectPackagesByAppId(appId)
            .stream()
            .map(softwareAssembler::toPackageResponse)
            .toList();
    }

    private SoftwareResponse freshDetail(Long id) {
        return softwareAssembler.toResponse(requireSoftware(id));
    }

    private SoftwareEntity requireSoftware(Long id) {
        requireValidId(id);
        SoftwareEntity software = softwareMapper.selectById(id);
        if (software == null) {
            throw new BusinessException(ErrorCode.SOFTWARE_NOT_FOUND, "软件不存在");
        }
        return software;
    }

    private AppVersionEntity requireVersion(Long appId, Long versionId) {
        requireValidId(versionId);
        AppVersionEntity version = softwareMapper.selectVersionById(versionId);
        if (version == null || !appId.equals(version.getAppId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "版本不存在");
        }
        return version;
    }

    private AppPackageEntity requirePackage(Long appId, Long packageId) {
        requireValidId(packageId);
        AppPackageEntity packageInfo = softwareMapper.selectPackageById(packageId);
        if (packageInfo == null || !appId.equals(packageInfo.getAppId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "安装包不存在");
        }
        return packageInfo;
    }

    private void requireValidId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "参数格式错误");
        }
    }

    private void rejectDirectPublish(boolean publishNow) {
        if (publishNow) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "直接发布已关闭，请先提交审核并在审核通过后上架");
        }
    }

    private void normalizeQuery(SoftwareQueryRequest request) {
        if (request.getPage() == null || request.getPage() < 1) {
            request.setPage(1);
        }
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(20);
        }
        if (request.getPageSize() > 100) {
            request.setPageSize(100);
        }
        if (StringUtils.hasText(request.getKeyword())) {
            request.setKeyword(request.getKeyword().trim());
        }
    }

    private Long normalizeAdminUserId(Long adminUserId) {
        return adminUserId == null || adminUserId <= 0 ? 0L : adminUserId;
    }

    private void validateTags(List<Long> tagIds) {
        for (Long tagId : tagIds) {
            if (softwareMapper.countTag(tagId) == 0) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "标签不存在: " + tagId);
            }
        }
    }

    private List<Long> parseTagIds(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String item : value.split(",")) {
            String text = item.trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            try {
                Long id = Long.parseLong(text);
                if (id <= 0) {
                    throw new NumberFormatException("tag id must be positive");
                }
                ids.add(id);
            } catch (NumberFormatException ex) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "标签ID格式错误");
            }
        }
        return new ArrayList<>(ids);
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Integer normalizeFlag(Integer value, Integer defaultValue, String message) {
        if (value == null) {
            return defaultValue == null ? 0 : defaultValue;
        }
        if (value != 0 && value != 1) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, message);
        }
        return value;
    }

    private String normalizeCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return String.join(",", splitCsv(value));
    }

    private String normalizeScreenshots(String value) {
        if (!StringUtils.hasText(value)) {
            return "[]";
        }
        String text = value.trim();
        if (text.startsWith("[")) {
            try {
                objectMapper.readValue(text, new TypeReference<List<String>>() {});
                return text;
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "截图JSON格式错误");
            }
        }
        try {
            return objectMapper.writeValueAsString(splitCsv(text));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "截图数据处理失败");
        }
    }

    private List<String> splitCsv(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
    }

    private boolean isDirectPackageUpload(MultipartFile packageFile, String uploadSessionId) {
        return packageFile != null && !packageFile.isEmpty() && !StringUtils.hasText(uploadSessionId);
    }

    private void cleanupDirectPackageIfNecessary(boolean cleanupOnFailure, VerifiedPackage verifiedPackage) {
        if (cleanupOnFailure) {
            packagePreparationService.deleteStoredPackageQuietly(verifiedPackage);
        }
    }
}
