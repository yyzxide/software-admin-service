package com.xcappstore.admin.software.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SoftwareAssembler {
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_REVIEWING = 1;
    private static final int STATUS_PUBLISHED = 2;
    private static final int STATUS_UNPUBLISHED = 3;
    private static final int STATUS_REJECTED = 4;
    private static final int VERSION_STATUS_DRAFT = 0;
    private static final int VERSION_STATUS_APPROVED = 2;
    private static final int PACKAGE_STATUS_AVAILABLE = 1;

    private final ObjectMapper objectMapper;

    public SoftwareAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SoftwareResponse toResponse(SoftwareEntity entity) {
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

    public AppVersionResponse toVersionResponse(AppVersionEntity entity) {
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

    public AppPackageResponse toPackageResponse(AppPackageEntity entity) {
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
            case 3 -> "扫描失败";
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
