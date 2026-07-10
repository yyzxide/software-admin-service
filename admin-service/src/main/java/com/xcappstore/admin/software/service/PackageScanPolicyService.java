package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.model.PackageScanStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackageScanPolicyService {
    public Integer parseScanResult(String result) {
        if (!StringUtils.hasText(result)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安全状态结果不能为空");
        }
        return switch (result.trim().toLowerCase()) {
            case "safe", "pass", "passed", "安全" -> PackageScanStatus.SAFE.code();
            case "risk", "risky", "danger", "风险" -> PackageScanStatus.RISKY.code();
            case "failed", "fail", "error", "失败" -> PackageScanStatus.FAILED.code();
            default -> throw new BusinessException(ErrorCode.PARAM_FORMAT, "安全状态结果只能是 safe、risky 或 failed");
        };
    }

    public String defaultScanReport(Integer scanStatus) {
        return switch (PackageScanStatus.fromCode(scanStatus)) {
            case SAFE -> "安装包安全状态标记为通过";
            case RISKY -> "安装包安全状态标记为有风险";
            case FAILED -> "安装包安全状态处理失败";
            default -> "安装包安全状态未知";
        };
    }
}
