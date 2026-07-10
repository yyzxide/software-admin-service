package com.xcappstore.admin.review.model;

import java.util.Arrays;

public enum ReviewTaskStatus {
    PENDING(0, "待审核"),
    REVIEWING(1, "审核中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    CANCELED(4, "已取消"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    ReviewTaskStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public boolean canDecide() {
        return this == PENDING || this == REVIEWING;
    }

    public static ReviewTaskStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
