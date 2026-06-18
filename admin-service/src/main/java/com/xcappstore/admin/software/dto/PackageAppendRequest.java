package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public class PackageAppendRequest {
    @NotBlank(message = "系统类型不能为空")
    private String osType;

    @NotBlank(message = "CPU架构不能为空")
    private String arch;

    @NotBlank(message = "包格式不能为空")
    private String packageFormat;

    private String uploadSessionId;
    private String expectedSha256;
    private String signatureAlgorithm;
    private String signatureValue;

    private MultipartFile packageFile;

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public void setOs_type(String osType) {
        this.osType = osType;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
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

    public String getUploadSessionId() {
        return uploadSessionId;
    }

    public void setUploadSessionId(String uploadSessionId) {
        this.uploadSessionId = uploadSessionId;
    }

    public void setUpload_session_id(String uploadSessionId) {
        this.uploadSessionId = uploadSessionId;
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

    public MultipartFile getPackageFile() {
        return packageFile;
    }

    public void setPackageFile(MultipartFile packageFile) {
        this.packageFile = packageFile;
    }

    public void setPackage_file(MultipartFile packageFile) {
        this.packageFile = packageFile;
    }
}
