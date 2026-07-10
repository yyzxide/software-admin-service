package com.xcappstore.admin.software.model;

import java.util.Arrays;

public enum AppVersionStatus {
    DRAFT(0, "草稿"),
    REVIEWING(1, "审核中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "审核驳回"),
    UNPUBLISHED(4, "已下架"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    AppVersionStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public static AppVersionStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
