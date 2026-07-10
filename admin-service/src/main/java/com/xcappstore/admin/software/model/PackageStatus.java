package com.xcappstore.admin.software.model;

import java.util.Arrays;

public enum PackageStatus {
    UPLOADING(0, "上传中"),
    AVAILABLE(1, "可用"),
    DELETED(2, "已删除"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    PackageStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public static PackageStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
