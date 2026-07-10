package com.xcappstore.admin.software.model;

import java.util.Arrays;

public enum PackageScanStatus {
    UNSCANNED(0, "未标记安全"),
    SAFE(1, "安全"),
    RISKY(2, "安全状态有风险"),
    FAILED(3, "安全状态处理失败"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    PackageScanStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public static PackageScanStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
