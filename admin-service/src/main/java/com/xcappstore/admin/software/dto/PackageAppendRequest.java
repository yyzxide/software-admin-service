package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class PackageAppendRequest {
    @NotBlank(message = "系统类型不能为空")
    private String osType;

    @NotBlank(message = "CPU架构不能为空")
    private String arch;

    @NotBlank(message = "包格式不能为空")
    private String packageFormat;

    @NotNull(message = "安装包文件不能为空")
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
