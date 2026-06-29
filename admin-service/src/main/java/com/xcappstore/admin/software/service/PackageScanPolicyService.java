package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackageScanPolicyService {
    public static final int SCAN_STATUS_SAFE = 1;
    public static final int SCAN_STATUS_RISKY = 2;
    public static final int SCAN_STATUS_FAILED = 3;

    public Integer parseScanResult(String result) {
        if (!StringUtils.hasText(result)) {
            return SCAN_STATUS_SAFE;
        }
        return switch (result.trim().toLowerCase()) {
            case "safe", "pass", "passed", "安全" -> SCAN_STATUS_SAFE;
            case "risk", "risky", "danger", "风险" -> SCAN_STATUS_RISKY;
            case "failed", "fail", "error", "失败" -> SCAN_STATUS_FAILED;
            default -> throw new BusinessException(ErrorCode.PARAM_FORMAT, "扫描结果只能是 safe、risky 或 failed");
        };
    }

    public String defaultScanReport(Integer scanStatus) {
        return switch (scanStatus) {
            case SCAN_STATUS_SAFE -> "本地模拟扫描通过";
            case SCAN_STATUS_RISKY -> "本地模拟扫描发现风险";
            case SCAN_STATUS_FAILED -> "本地模拟扫描失败";
            default -> "本地模拟扫描结果未知";
        };
    }
}
