package com.xcappstore.admin.software.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.service.OperationLogService;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.PackageAppendRequest;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareUpdateRequest;
import com.xcappstore.admin.software.dto.VersionCreateRequest;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import com.xcappstore.admin.software.service.PackageFileStorageService;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationRequest;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationResult;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import com.xcappstore.admin.software.service.PackageSecurityPolicyService;
import com.xcappstore.admin.software.service.PackageUploadSessionService;
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
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_REVIEWING = 1;
    private static final int STATUS_PUBLISHED = 2;
    private static final int STATUS_UNPUBLISHED = 3;
    private static final int STATUS_REJECTED = 4;
    private static final int VERSION_STATUS_DRAFT = 0;
    private static final int VERSION_STATUS_APPROVED = 2;
    private static final int PACKAGE_STATUS_AVAILABLE = 1;
    private static final String SUBMIT_SOURCE_ADMIN = "admin";
    private static final Set<String> ALLOWED_OS_TYPES = Set.of("uos_v20", "uos_v23", "kylin_v10", "kylin_v11");
    private static final Set<String> ALLOWED_ARCHS = Set.of("x86_64", "aarch64", "loongarch64");
    private static final Set<String> ALLOWED_PACKAGE_FORMATS = Set.of("deb", "rpm", "appimage");
    private static final Set<Integer> PUBLISHABLE_STATUSES = Set.of(
        STATUS_DRAFT,
        STATUS_REVIEWING,
        STATUS_PUBLISHED,
        STATUS_UNPUBLISHED,
        STATUS_REJECTED
    );
    private static final Set<Integer> UNPUBLISHABLE_STATUSES = Set.of(
        STATUS_REVIEWING,
        STATUS_PUBLISHED,
        STATUS_REJECTED
    );

    private final SoftwareMapper softwareMapper;
    private final SoftwareCacheService softwareCacheService;
    private final PackageFileStorageService packageFileStorageService;
    private final PackageUploadSessionService packageUploadSessionService;
    private final PackageSecurityPolicyService packageSecurityPolicyService;
    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    public SoftwareServiceImpl(
        SoftwareMapper softwareMapper,
        SoftwareCacheService softwareCacheService,
        PackageFileStorageService packageFileStorageService,
        PackageUploadSessionService packageUploadSessionService,
        PackageSecurityPolicyService packageSecurityPolicyService,
        OperationLogService operationLogService,
        ObjectMapper objectMapper
    ) {
        this.softwareMapper = softwareMapper;
        this.softwareCacheService = softwareCacheService;
        this.packageFileStorageService = packageFileStorageService;
        this.packageUploadSessionService = packageUploadSessionService;
        this.packageSecurityPolicyService = packageSecurityPolicyService;
        this.operationLogService = operationLogService;
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

        String osType = requireAllowed(normalizeText(request.getOsType()), ALLOWED_OS_TYPES, "系统类型不支持");
        String arch = requireAllowed(normalizeText(request.getArch()), ALLOWED_ARCHS, "CPU架构不支持");
        String packageFormat = requireAllowed(normalizeText(request.getPackageFormat()), ALLOWED_PACKAGE_FORMATS, "安装包格式不支持");
        List<Long> tagIds = parseTagIds(request.getTagIds());
        validateTags(tagIds);

        VerifiedPackage verifiedPackage = resolvePackage(
            request.getPackageFile(),
            request.getUploadSessionId(),
            packageFormat,
            request.getExpectedSha256(),
            request.getSignatureAlgorithm(),
            request.getSignatureValue(),
            operatorId
        );
        PackageFileStorageService.StoredPackage storedPackage = verifiedPackage.storedPackage();
        LocalDateTime now = LocalDateTime.now();
        boolean publishNow = Boolean.TRUE.equals(request.getPublishNow());

        SoftwareEntity app = new SoftwareEntity();
        app.setAppKey(appKey);
        app.setName(normalizeText(request.getName()));
        app.setDeveloperId(operatorId);
        app.setSubmitSource(SUBMIT_SOURCE_ADMIN);
        app.setCategoryId(request.getCategoryId());
        app.setIconUrl(defaultText(normalizeText(request.getIconUrl()), ""));
        app.setSummary(normalizeText(request.getSummary()));
        app.setDescription(normalizeText(request.getDescription()));
        app.setSupportedOsTypes(defaultText(normalizeCsv(request.getSupportedOsTypes()), osType));
        app.setSupportedArchs(defaultText(normalizeCsv(request.getSupportedArchs()), arch));
        app.setScreenshots(normalizeScreenshots(request.getScreenshots()));
        app.setStatus(publishNow ? STATUS_PUBLISHED : STATUS_DRAFT);
        app.setIsOfficial(0);
        app.setIsFeatured(0);
        app.setSortWeight(0);
        app.setDownloadCount(0L);
        app.setRatingScore(BigDecimal.ZERO);
        app.setRatingCount(0);
        app.setPublishedAt(publishNow ? now : null);
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
        version.setStatus(publishNow ? VERSION_STATUS_APPROVED : VERSION_STATUS_DRAFT);
        version.setIsLatest(1);
        version.setSubmittedAt(now);
        version.setReviewedAt(publishNow ? now : null);
        version.setPublishedAt(publishNow ? now : null);
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        version.setCreatedBy(operatorId);
        version.setUpdatedBy(operatorId);
        softwareMapper.insertVersion(version);

        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setAppId(app.getId());
        packageInfo.setVersionId(version.getId());
        packageInfo.setOsType(osType);
        packageInfo.setArch(arch);
        packageInfo.setPackageFormat(packageFormat);
        packageInfo.setFileName(storedPackage.fileName());
        packageInfo.setFileSize(storedPackage.fileSize());
        packageInfo.setStoragePath(storedPackage.storagePath());
        packageInfo.setSha256(storedPackage.sha256());
        applyPackageVerification(packageInfo, verifiedPackage.verificationResult());
        packageInfo.setStatus(PACKAGE_STATUS_AVAILABLE);
        packageInfo.setDownloadCount(0L);
        packageInfo.setScanStatus(0);
        packageInfo.setCreatedAt(now);
        packageInfo.setUpdatedAt(now);
        packageInfo.setCreatedBy(operatorId);
        packageInfo.setUpdatedBy(operatorId);
        softwareMapper.insertPackage(packageInfo);

        if (publishNow) {
            packageSecurityPolicyService.assertAppPackagesPublishable(app.getId());
        }

        for (Long tagId : tagIds) {
            softwareMapper.insertAppTag(app.getId(), tagId);
        }

        softwareCacheService.invalidateDetail(app.getId());
        SoftwareResponse response = detail(app.getId());
        operationLogService.record(operatorId, "software_upload", "software", app.getId(), app.getName(), "上传软件: " + app.getName());
        return response;
    }

    @Override
    public PageResponse<SoftwareResponse> page(SoftwareQueryRequest request) {
        normalizeQuery(request);
        long total = softwareMapper.countList(request);
        List<SoftwareResponse> list = total == 0 ? List.of() : softwareMapper
            .selectList(request, request.offset(), request.getPageSize())
            .stream()
            .map(this::toResponse)
            .toList();
        return new PageResponse<>(total, request.getPage(), request.getPageSize(), list);
    }

    @Override
    public SoftwareResponse detail(Long id) {
        requireValidId(id);
        return softwareCacheService.getDetail(id).orElseGet(() -> {
            SoftwareResponse response = toResponse(requireSoftware(id));
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
        SoftwareResponse response = detail(id);
        operationLogService.record(operatorId, "software_update", "software", id, response.getName(), "编辑软件资料: " + response.getName());
        return response;
    }

    @Override
    @Transactional
    public SoftwareResponse publish(Long id, Long adminUserId) {
        SoftwareEntity app = requireSoftware(id);
        if (!PUBLISHABLE_STATUSES.contains(app.getStatus())) {
            throw new BusinessException(ErrorCode.SOFTWARE_INVALID_STATUS, "当前状态不允许上架");
        }

        Long operatorId = normalizeAdminUserId(adminUserId);
        packageSecurityPolicyService.assertAppPackagesPublishable(id);
        LocalDateTime now = LocalDateTime.now();
        softwareMapper.updateStatus(id, STATUS_PUBLISHED, now, operatorId);
        softwareMapper.approveDraftVersions(id, now, now, operatorId);
        softwareCacheService.invalidateDetail(id);
        SoftwareResponse response = detail(id);
        operationLogService.record(operatorId, "software_publish", "software", id, response.getName(), "上架软件: " + response.getName());
        return response;
    }

    @Override
    @Transactional
    public SoftwareResponse unpublish(Long id, Long adminUserId) {
        SoftwareEntity app = requireSoftware(id);
        if (!UNPUBLISHABLE_STATUSES.contains(app.getStatus())) {
            throw new BusinessException(ErrorCode.SOFTWARE_INVALID_STATUS, "当前状态不允许下架");
        }

        Long operatorId = normalizeAdminUserId(adminUserId);
        softwareMapper.updateStatus(id, STATUS_UNPUBLISHED, app.getPublishedAt(), operatorId);
        softwareCacheService.invalidateDetail(id);
        SoftwareResponse response = detail(id);
        operationLogService.record(operatorId, "software_unpublish", "software", id, response.getName(), "下架软件: " + response.getName());
        return response;
    }

    @Override
    @Transactional
    public AppVersionResponse addVersion(Long appId, VersionCreateRequest request, Long adminUserId) {
        SoftwareEntity app = requireSoftware(appId);
        Long operatorId = normalizeAdminUserId(adminUserId);
        String osType = requireAllowed(normalizeText(request.getOsType()), ALLOWED_OS_TYPES, "系统类型不支持");
        String arch = requireAllowed(normalizeText(request.getArch()), ALLOWED_ARCHS, "CPU架构不支持");
        String packageFormat = requireAllowed(normalizeText(request.getPackageFormat()), ALLOWED_PACKAGE_FORMATS, "安装包格式不支持");
        Long versionCode = request.getVersionCode() == null ? System.currentTimeMillis() / 1000 : request.getVersionCode();
        if (versionCode <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "版本号必须大于0");
        }
        if (softwareMapper.countVersionCode(appId, versionCode) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "版本号已存在");
        }

        boolean publishNow = Boolean.TRUE.equals(request.getPublishNow());
        VerifiedPackage verifiedPackage = resolvePackage(
            request.getPackageFile(),
            request.getUploadSessionId(),
            packageFormat,
            request.getExpectedSha256(),
            request.getSignatureAlgorithm(),
            request.getSignatureValue(),
            operatorId
        );
        PackageFileStorageService.StoredPackage storedPackage = verifiedPackage.storedPackage();
        LocalDateTime now = LocalDateTime.now();

        if (publishNow) {
            softwareMapper.markVersionsNotLatest(appId, operatorId);
        }

        AppVersionEntity version = new AppVersionEntity();
        version.setAppId(appId);
        version.setVersionName(normalizeText(request.getVersionName()));
        version.setVersionCode(versionCode);
        version.setChangelog(normalizeText(request.getChangelog()));
        version.setSubmitSource(SUBMIT_SOURCE_ADMIN);
        version.setStatus(publishNow ? VERSION_STATUS_APPROVED : VERSION_STATUS_DRAFT);
        version.setIsLatest(publishNow ? 1 : 0);
        version.setSubmittedAt(now);
        version.setReviewedAt(publishNow ? now : null);
        version.setPublishedAt(publishNow ? now : null);
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        version.setCreatedBy(operatorId);
        version.setUpdatedBy(operatorId);
        softwareMapper.insertVersion(version);

        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setAppId(appId);
        packageInfo.setVersionId(version.getId());
        packageInfo.setOsType(osType);
        packageInfo.setArch(arch);
        packageInfo.setPackageFormat(packageFormat);
        packageInfo.setFileName(storedPackage.fileName());
        packageInfo.setFileSize(storedPackage.fileSize());
        packageInfo.setStoragePath(storedPackage.storagePath());
        packageInfo.setSha256(storedPackage.sha256());
        applyPackageVerification(packageInfo, verifiedPackage.verificationResult());
        packageInfo.setStatus(PACKAGE_STATUS_AVAILABLE);
        packageInfo.setDownloadCount(0L);
        packageInfo.setScanStatus(0);
        packageInfo.setCreatedAt(now);
        packageInfo.setUpdatedAt(now);
        packageInfo.setCreatedBy(operatorId);
        packageInfo.setUpdatedBy(operatorId);
        softwareMapper.insertPackage(packageInfo);

        if (publishNow) {
            packageSecurityPolicyService.assertVersionPackagesPublishable(appId, version.getId());
            softwareMapper.updateStatus(app.getId(), STATUS_PUBLISHED, now, operatorId);
        }

        softwareCacheService.invalidateDetail(appId);
        operationLogService.record(operatorId, "software_version_create", "software_version", version.getId(), app.getName() + " " + version.getVersionName(), "新增软件版本: " + app.getName() + " " + version.getVersionName());
        AppVersionResponse response = toVersionResponse(version);
        response.setPackages(List.of(toPackageResponse(packageInfo)));
        return response;
    }

    @Override
    @Transactional
    public AppPackageResponse addPackage(Long appId, Long versionId, PackageAppendRequest request, Long adminUserId) {
        requireSoftware(appId);
        requireVersion(appId, versionId);
        Long operatorId = normalizeAdminUserId(adminUserId);
        String osType = requireAllowed(normalizeText(request.getOsType()), ALLOWED_OS_TYPES, "系统类型不支持");
        String arch = requireAllowed(normalizeText(request.getArch()), ALLOWED_ARCHS, "CPU架构不支持");
        String packageFormat = requireAllowed(normalizeText(request.getPackageFormat()), ALLOWED_PACKAGE_FORMATS, "安装包格式不支持");
        if (softwareMapper.countPackageVariant(versionId, osType, arch) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "该版本已存在相同系统和架构的安装包");
        }

        VerifiedPackage verifiedPackage = resolvePackage(
            request.getPackageFile(),
            request.getUploadSessionId(),
            packageFormat,
            request.getExpectedSha256(),
            request.getSignatureAlgorithm(),
            request.getSignatureValue(),
            operatorId
        );
        PackageFileStorageService.StoredPackage storedPackage = verifiedPackage.storedPackage();
        LocalDateTime now = LocalDateTime.now();
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setAppId(appId);
        packageInfo.setVersionId(versionId);
        packageInfo.setOsType(osType);
        packageInfo.setArch(arch);
        packageInfo.setPackageFormat(packageFormat);
        packageInfo.setFileName(storedPackage.fileName());
        packageInfo.setFileSize(storedPackage.fileSize());
        packageInfo.setStoragePath(storedPackage.storagePath());
        packageInfo.setSha256(storedPackage.sha256());
        applyPackageVerification(packageInfo, verifiedPackage.verificationResult());
        packageInfo.setStatus(PACKAGE_STATUS_AVAILABLE);
        packageInfo.setDownloadCount(0L);
        packageInfo.setScanStatus(0);
        packageInfo.setCreatedAt(now);
        packageInfo.setUpdatedAt(now);
        packageInfo.setCreatedBy(operatorId);
        packageInfo.setUpdatedBy(operatorId);
        softwareMapper.insertPackage(packageInfo);

        softwareCacheService.invalidateDetail(appId);
        operationLogService.record(operatorId, "software_package_create", "software_package", packageInfo.getId(), packageInfo.getFileName(), "新增安装包: " + packageInfo.getFileName());
        return toPackageResponse(packageInfo);
    }

    @Override
    public List<AppVersionResponse> versions(Long appId) {
        requireSoftware(appId);
        Map<Long, List<AppPackageResponse>> packagesByVersion = softwareMapper.selectPackagesByAppId(appId)
            .stream()
            .map(this::toPackageResponse)
            .collect(Collectors.groupingBy(AppPackageResponse::getVersionId));
        return softwareMapper.selectVersionsByAppId(appId)
            .stream()
            .map(version -> {
                AppVersionResponse response = toVersionResponse(version);
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
            .map(this::toPackageResponse)
            .toList();
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

    private void requireValidId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "参数格式错误");
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

    private String requireAllowed(String value, Set<String> allowedValues, String message) {
        if (!StringUtils.hasText(value) || !allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, message);
        }
        return value;
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

    private VerifiedPackage resolvePackage(
        MultipartFile packageFile,
        String uploadSessionId,
        String packageFormat,
        String expectedSha256,
        String signatureAlgorithm,
        String signatureValue,
        Long operatorId
    ) {
        boolean hasUploadSession = StringUtils.hasText(uploadSessionId);
        boolean hasDirectFile = packageFile != null && !packageFile.isEmpty();
        if (hasUploadSession && hasDirectFile) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包文件和上传会话不能同时提交");
        }
        if (hasUploadSession) {
            return packageUploadSessionService.consumeCompletedSession(uploadSessionId, packageFormat, operatorId);
        }
        return packageFileStorageService.storeAndVerify(
            packageFile,
            packageFormat,
            new PackageVerificationRequest(expectedSha256, signatureAlgorithm, signatureValue)
        );
    }

    private void applyPackageVerification(AppPackageEntity packageInfo, PackageVerificationResult verificationResult) {
        PackageVerificationResult result = verificationResult == null
            ? PackageVerificationResult.notVerified()
            : verificationResult;
        packageInfo.setSignatureAlgorithm(result.signatureAlgorithm());
        packageInfo.setSignatureValue(result.signatureValue());
        packageInfo.setSignatureStatus(result.signatureStatus());
        packageInfo.setSignatureVerifiedAt(result.signatureVerifiedAt());
    }

    private SoftwareResponse toResponse(SoftwareEntity entity) {
        SoftwareResponse response = new SoftwareResponse();
        response.setId(entity.getId());
        response.setAppKey(entity.getAppKey());
        response.setName(entity.getName());
        response.setDeveloperId(entity.getDeveloperId());
        response.setSubmitSource(entity.getSubmitSource());
        response.setCategoryId(entity.getCategoryId());
        response.setCategoryName(entity.getCategoryName());
        response.setIconUrl(entity.getIconUrl());
        response.setSummary(entity.getSummary());
        response.setDescription(entity.getDescription());
        response.setSupportedOsTypes(splitCsv(entity.getSupportedOsTypes()));
        response.setSupportedArchs(splitCsv(entity.getSupportedArchs()));
        response.setScreenshots(parseScreenshots(entity.getScreenshots()));
        response.setStatus(entity.getStatus());
        response.setStatusText(statusText(entity.getStatus()));
        response.setIsOfficial(entity.getIsOfficial());
        response.setIsFeatured(entity.getIsFeatured());
        response.setSortWeight(entity.getSortWeight());
        response.setDownloadCount(entity.getDownloadCount());
        response.setRatingScore(entity.getRatingScore());
        response.setRatingCount(entity.getRatingCount());
        response.setLatestVersionName(entity.getLatestVersionName());
        response.setPackageCount(entity.getPackageCount() == null ? 0L : entity.getPackageCount());
        response.setTagNames(splitCsv(entity.getTagNames()));
        response.setPublishedAt(entity.getPublishedAt());
        response.setRejectedAt(entity.getRejectedAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private AppVersionResponse toVersionResponse(AppVersionEntity entity) {
        AppVersionResponse response = new AppVersionResponse();
        response.setId(entity.getId());
        response.setAppId(entity.getAppId());
        response.setVersionName(entity.getVersionName());
        response.setVersionCode(entity.getVersionCode());
        response.setChangelog(entity.getChangelog());
        response.setSubmitSource(entity.getSubmitSource());
        response.setStatus(entity.getStatus());
        response.setStatusText(versionStatusText(entity.getStatus()));
        response.setIsLatest(entity.getIsLatest());
        response.setSubmittedAt(entity.getSubmittedAt());
        response.setReviewedAt(entity.getReviewedAt());
        response.setPublishedAt(entity.getPublishedAt());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private AppPackageResponse toPackageResponse(AppPackageEntity entity) {
        AppPackageResponse response = new AppPackageResponse();
        response.setId(entity.getId());
        response.setAppId(entity.getAppId());
        response.setVersionId(entity.getVersionId());
        response.setOsType(entity.getOsType());
        response.setArch(entity.getArch());
        response.setPackageFormat(entity.getPackageFormat());
        response.setFileName(entity.getFileName());
        response.setFileSize(entity.getFileSize());
        response.setStoragePath(entity.getStoragePath());
        response.setCdnUrl(entity.getCdnUrl());
        response.setSha256(entity.getSha256());
        response.setSignatureAlgorithm(entity.getSignatureAlgorithm());
        response.setSignatureStatus(entity.getSignatureStatus());
        response.setSignatureStatusText(signatureStatusText(entity.getSignatureStatus()));
        response.setSignatureVerifiedAt(entity.getSignatureVerifiedAt());
        response.setStatus(entity.getStatus());
        response.setStatusText(packageStatusText(entity.getStatus()));
        response.setDownloadCount(entity.getDownloadCount());
        response.setScanStatus(entity.getScanStatus());
        response.setScanStatusText(scanStatusText(entity.getScanStatus()));
        response.setScanReport(entity.getScanReport());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
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

    private List<String> parseScreenshots(String screenshots) {
        if (!StringUtils.hasText(screenshots)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(screenshots, new TypeReference<>() {});
        } catch (Exception ex) {
            return splitCsv(screenshots);
        }
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case STATUS_DRAFT -> "草稿";
            case STATUS_REVIEWING -> "审核中";
            case STATUS_PUBLISHED -> "已上架";
            case STATUS_UNPUBLISHED -> "已下架";
            case STATUS_REJECTED -> "审核驳回";
            default -> "未知";
        };
    }

    private String versionStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case VERSION_STATUS_DRAFT -> "草稿";
            case 1 -> "审核中";
            case VERSION_STATUS_APPROVED -> "已通过";
            case 3 -> "审核驳回";
            case 4 -> "已下架";
            default -> "未知";
        };
    }

    private String packageStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "上传中";
            case PACKAGE_STATUS_AVAILABLE -> "可用";
            case 2 -> "已删除";
            default -> "未知";
        };
    }

    private String scanStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "未扫描";
            case 1 -> "安全";
            case 2 -> "有风险";
            default -> "未知";
        };
    }

    private String signatureStatusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 0 -> "未校验";
            case 1 -> "通过";
            case 2 -> "失败";
            default -> "未知";
        };
    }
}
