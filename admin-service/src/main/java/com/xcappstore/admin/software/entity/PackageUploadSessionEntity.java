package com.xcappstore.admin.software.entity;

import java.time.LocalDateTime;

public class PackageUploadSessionEntity {
    private String uploadId;
    private String fileName;
    private String packageFormat;
    private Long fileSize;
    private Long chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunkCount;
    private String uploadedChunks;
    private String expectedSha256;
    private String actualSha256;
    private String storagePath;
    private String signatureAlgorithm;
    private String signatureValue;
    private Integer signatureStatus;
    private LocalDateTime signatureVerifiedAt;
    private Integer status;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageFormat() {
        return packageFormat;
    }

    public void setPackageFormat(String packageFormat) {
        this.packageFormat = packageFormat;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public Integer getUploadedChunkCount() {
        return uploadedChunkCount;
    }

    public void setUploadedChunkCount(Integer uploadedChunkCount) {
        this.uploadedChunkCount = uploadedChunkCount;
    }

    public String getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(String uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public void setExpectedSha256(String expectedSha256) {
        this.expectedSha256 = expectedSha256;
    }

    public String getActualSha256() {
        return actualSha256;
    }

    public void setActualSha256(String actualSha256) {
        this.actualSha256 = actualSha256;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSignatureValue() {
        return signatureValue;
    }

    public void setSignatureValue(String signatureValue) {
        this.signatureValue = signatureValue;
    }

    public Integer getSignatureStatus() {
        return signatureStatus;
    }

    public void setSignatureStatus(Integer signatureStatus) {
        this.signatureStatus = signatureStatus;
    }

    public LocalDateTime getSignatureVerifiedAt() {
        return signatureVerifiedAt;
    }

    public void setSignatureVerifiedAt(LocalDateTime signatureVerifiedAt) {
        this.signatureVerifiedAt = signatureVerifiedAt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
