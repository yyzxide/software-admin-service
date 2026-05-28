package com.xcappstore.admin.software.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import com.xcappstore.admin.software.service.PackageFileStorageService;
import com.xcappstore.admin.software.service.SoftwareCacheService;
import com.xcappstore.admin.software.service.SoftwareService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final ObjectMapper objectMapper;

    public SoftwareServiceImpl(
        SoftwareMapper softwareMapper,
        SoftwareCacheService softwareCacheService,
        PackageFileStorageService packageFileStorageService,
        ObjectMapper objectMapper
    ) {
        this.softwareMapper = softwareMapper;
        this.softwareCacheService = softwareCacheService;
        this.packageFileStorageService = packageFileStorageService;
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

        PackageFileStorageService.StoredPackage storedPackage = packageFileStorageService.store(request.getPackageFile());
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
        packageInfo.setStatus(PACKAGE_STATUS_AVAILABLE);
        packageInfo.setDownloadCount(0L);
        packageInfo.setScanStatus(0);
        packageInfo.setCreatedAt(now);
        packageInfo.setUpdatedAt(now);
        packageInfo.setCreatedBy(operatorId);
        packageInfo.setUpdatedBy(operatorId);
        softwareMapper.insertPackage(packageInfo);

        for (Long tagId : tagIds) {
            softwareMapper.insertAppTag(app.getId(), tagId);
        }

        softwareCacheService.invalidateDetail(app.getId());
        return detail(app.getId());
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
    public SoftwareResponse publish(Long id, Long adminUserId) {
        SoftwareEntity app = requireSoftware(id);
        if (!PUBLISHABLE_STATUSES.contains(app.getStatus())) {
            throw new BusinessException(ErrorCode.SOFTWARE_INVALID_STATUS, "当前状态不允许上架");
        }

        LocalDateTime now = LocalDateTime.now();
        softwareMapper.updateStatus(id, STATUS_PUBLISHED, now, normalizeAdminUserId(adminUserId));
        softwareMapper.approveDraftVersions(id, now, now, normalizeAdminUserId(adminUserId));
        softwareCacheService.invalidateDetail(id);
        return detail(id);
    }

    @Override
    @Transactional
    public SoftwareResponse unpublish(Long id, Long adminUserId) {
        SoftwareEntity app = requireSoftware(id);
        if (!UNPUBLISHABLE_STATUSES.contains(app.getStatus())) {
            throw new BusinessException(ErrorCode.SOFTWARE_INVALID_STATUS, "当前状态不允许下架");
        }

        softwareMapper.updateStatus(id, STATUS_UNPUBLISHED, app.getPublishedAt(), normalizeAdminUserId(adminUserId));
        softwareCacheService.invalidateDetail(id);
        return detail(id);
    }

    private SoftwareEntity requireSoftware(Long id) {
        requireValidId(id);
        SoftwareEntity software = softwareMapper.selectById(id);
        if (software == null) {
            throw new BusinessException(ErrorCode.SOFTWARE_NOT_FOUND, "软件不存在");
        }
        return software;
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
}
