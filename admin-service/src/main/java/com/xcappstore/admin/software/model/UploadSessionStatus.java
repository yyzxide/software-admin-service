package com.xcappstore.admin.software.model;

import java.util.Arrays;

public enum UploadSessionStatus {
    UPLOADING(0, "上传中"),
    COMPLETED(1, "已完成"),
    CONSUMED(2, "已使用"),
    FAILED(3, "失败"),
    COMPLETING(4, "合并中"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String text;

    UploadSessionStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public boolean isUploading() {
        return this == UPLOADING;
    }

    public static UploadSessionStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
            .filter(status -> status.code == code)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
