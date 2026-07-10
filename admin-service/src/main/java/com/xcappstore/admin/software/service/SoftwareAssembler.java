package com.xcappstore.admin.software.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import com.xcappstore.admin.software.model.AppVersionStatus;
import com.xcappstore.admin.software.model.PackageScanStatus;
import com.xcappstore.admin.software.model.PackageStatus;
import com.xcappstore.admin.software.model.SignatureStatus;
import com.xcappstore.admin.software.model.SoftwareStatus;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SoftwareAssembler {
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
        response.setStatusText(SoftwareStatus.fromCode(entity.getStatus()).text());
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
        response.setStatusText(AppVersionStatus.fromCode(entity.getStatus()).text());
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
        response.setSignatureStatusText(SignatureStatus.fromCode(entity.getSignatureStatus()).text());
        response.setSignatureVerifiedAt(entity.getSignatureVerifiedAt());
        response.setStatus(entity.getStatus());
        response.setStatusText(PackageStatus.fromCode(entity.getStatus()).text());
        response.setDownloadCount(entity.getDownloadCount());
        response.setScanStatus(entity.getScanStatus());
        response.setScanStatusText(PackageScanStatus.fromCode(entity.getScanStatus()).text());
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
}
