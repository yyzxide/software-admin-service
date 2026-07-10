package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class SoftwareUploadRequest {
    @NotBlank(message = "软件标识不能为空")
    @Size(max = 128, message = "软件标识不能超过128个字符")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$", message = "软件标识只能包含字母、数字、点、下划线和短横线")
    private String appKey;

    @NotBlank(message = "软件名称不能为空")
    @Size(max = 128, message = "软件名称不能超过128个字符")
    private String name;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Size(max = 512, message = "图标地址不能超过512个字符")
    private String iconUrl;

    @NotBlank(message = "软件摘要不能为空")
    @Size(max = 512, message = "软件摘要不能超过512个字符")
    private String summary;

    @NotBlank(message = "软件描述不能为空")
    private String description;

    @NotBlank(message = "系统类型不能为空")
    private String osType;

    @NotBlank(message = "CPU架构不能为空")
    private String arch;

    @NotBlank(message = "包格式不能为空")
    private String packageFormat;

    @NotBlank(message = "版本名称不能为空")
    @Size(max = 64, message = "版本名称不能超过64个字符")
    private String versionName;

    @Positive(message = "版本号必须大于0")
    private Long versionCode;
    private String supportedOsTypes;
    private String supportedArchs;
    private String screenshots;
    private String tagIds;
    private Boolean publishNow;
    private String uploadSessionId;
    @Pattern(regexp = "^$|^[0-9a-fA-F]{64}$", message = "SHA256格式错误")
    private String expectedSha256;
    private String signatureAlgorithm;
    private String signatureValue;

    private MultipartFile packageFile;

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public void setApp_key(String appKey) {
        this.appKey = appKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategory_id(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public void setIcon_url(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

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

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public void setVersion_name(String versionName) {
        this.versionName = versionName;
    }

    public Long getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(Long versionCode) {
        this.versionCode = versionCode;
    }

    public void setVersion_code(Long versionCode) {
        this.versionCode = versionCode;
    }

    public String getSupportedOsTypes() {
        return supportedOsTypes;
    }

    public void setSupportedOsTypes(String supportedOsTypes) {
        this.supportedOsTypes = supportedOsTypes;
    }

    public void setSupported_os_types(String supportedOsTypes) {
        this.supportedOsTypes = supportedOsTypes;
    }

    public String getSupportedArchs() {
        return supportedArchs;
    }

    public void setSupportedArchs(String supportedArchs) {
        this.supportedArchs = supportedArchs;
    }

    public void setSupported_archs(String supportedArchs) {
        this.supportedArchs = supportedArchs;
    }

    public String getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(String screenshots) {
        this.screenshots = screenshots;
    }

    public String getTagIds() {
        return tagIds;
    }

    public void setTagIds(String tagIds) {
        this.tagIds = tagIds;
    }

    public void setTag_ids(String tagIds) {
        this.tagIds = tagIds;
    }

    public Boolean getPublishNow() {
        return publishNow;
    }

    public void setPublishNow(Boolean publishNow) {
        this.publishNow = publishNow;
    }

    public void setPublish_now(Boolean publishNow) {
        this.publishNow = publishNow;
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
