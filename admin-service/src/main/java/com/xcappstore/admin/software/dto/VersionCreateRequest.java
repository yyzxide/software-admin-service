package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class VersionCreateRequest {
    @NotBlank(message = "版本名称不能为空")
    @Size(max = 64, message = "版本名称不能超过64个字符")
    private String versionName;

    private Long versionCode;
    private String changelog;

    @NotBlank(message = "系统类型不能为空")
    private String osType;

    @NotBlank(message = "CPU架构不能为空")
    private String arch;

    @NotBlank(message = "包格式不能为空")
    private String packageFormat;

    private Boolean publishNow;

    @NotNull(message = "安装包文件不能为空")
    private MultipartFile packageFile;

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

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
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

    public Boolean getPublishNow() {
        return publishNow;
    }

    public void setPublishNow(Boolean publishNow) {
        this.publishNow = publishNow;
    }

    public void setPublish_now(Boolean publishNow) {
        this.publishNow = publishNow;
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
