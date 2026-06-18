package com.xcappstore.admin.software.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PackageUploadSessionResponse {
    private String uploadId;
    private String fileName;
    private String packageFormat;
    private Long fileSize;
    private Long chunkSize;
    private Integer totalChunks;
    private Integer uploadedChunkCount;
    private List<Integer> uploadedChunks = new ArrayList<>();
    private String expectedSha256;
    private String actualSha256;
    private String storagePath;
    private String signatureAlgorithm;
    private Integer signatureStatus;
    private String signatureStatusText;
    private Integer status;
    private String statusText;
    private String errorMessage;
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

    public List<Integer> getUploadedChunks() {
        return uploadedChunks;
    }

    public void setUploadedChunks(List<Integer> uploadedChunks) {
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

    public Integer getSignatureStatus() {
        return signatureStatus;
    }

    public void setSignatureStatus(Integer signatureStatus) {
        this.signatureStatus = signatureStatus;
    }

    public String getSignatureStatusText() {
        return signatureStatusText;
    }

    public void setSignatureStatusText(String signatureStatusText) {
        this.signatureStatusText = signatureStatusText;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
