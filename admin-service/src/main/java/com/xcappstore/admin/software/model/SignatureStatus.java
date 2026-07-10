package com.xcappstore.admin.software.model;

import java.util.Arrays;

public enum SignatureStatus {
    NOT_VERIFIED(0, "未校验"),
    VERIFIED(1, "通过"),
    FAILED(2, "失败"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    SignatureStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public static SignatureStatus fromCode(Integer code) {
        if (code == null) {
            return NOT_VERIFIED;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
