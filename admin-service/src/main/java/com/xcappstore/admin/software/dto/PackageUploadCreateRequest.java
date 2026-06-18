package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PackageUploadCreateRequest {
    @NotBlank(message = "文件名不能为空")
    @Size(max = 256, message = "文件名不能超过256个字符")
    private String fileName;

    @NotBlank(message = "包格式不能为空")
    private String packageFormat;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    @NotNull(message = "分片大小不能为空")
    @Min(value = 1, message = "分片大小必须大于0")
    private Long chunkSize;

    @Size(max = 128, message = "SHA256 不能超过128个字符")
    private String expectedSha256;

    @Size(max = 64, message = "签名算法不能超过64个字符")
    private String signatureAlgorithm;

    @Size(max = 2048, message = "签名值不能超过2048个字符")
    private String signatureValue;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFile_name(String fileName) {
        this.fileName = fileName;
    }

    public String getPackageFormat() {
        return packageFormat;
    }

    public void setPackageFormat(String packageFormat) {
        this.packageFormat = packageFormat;
    }

    public void setPackage_format(String packageFormat) {
        this.packageFormat = packageFormat;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFile_size(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setChunk_size(Long chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public void setExpectedSha256(String expectedSha256) {
        this.expectedSha256 = expectedSha256;
    }

    public void setExpected_sha256(String expectedSha256) {
        this.expectedSha256 = expectedSha256;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public void setSignature_algorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSignatureValue() {
        return signatureValue;
    }

    public void setSignatureValue(String signatureValue) {
        this.signatureValue = signatureValue;
    }

    public void setSignature_value(String signatureValue) {
        this.signatureValue = signatureValue;
    }
}
