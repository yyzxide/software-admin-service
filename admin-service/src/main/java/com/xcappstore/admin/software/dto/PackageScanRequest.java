package com.xcappstore.admin.software.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public class PackageScanRequest {
    @NotBlank(message = "安全状态结果不能为空")
    @Size(max = 16, message = "安全状态结果不能超过16个字符")
    private String result;

    @Size(max = 2000, message = "安全状态说明不能超过2000个字符")
    private String report;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }
}
