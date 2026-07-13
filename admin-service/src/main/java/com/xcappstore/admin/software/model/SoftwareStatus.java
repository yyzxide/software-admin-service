package com.xcappstore.admin.software.model;

import java.util.Arrays;

public enum SoftwareStatus {
    DRAFT(0, "草稿"),
    REVIEWING(1, "审核中"),
    PUBLISHED(2, "已上架"),
    UNPUBLISHED(3, "已下架"),
    REJECTED(4, "审核驳回"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    SoftwareStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public boolean canPublish() {
        return this == UNPUBLISHED;
    }

    public boolean canUnpublish() {
        return this == PUBLISHED;
    }

    public boolean canSubmitReview() {
        return this == DRAFT || this == UNPUBLISHED || this == REJECTED;
    }

    public static SoftwareStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
