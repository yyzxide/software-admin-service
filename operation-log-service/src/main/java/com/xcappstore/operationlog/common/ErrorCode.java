package com.xcappstore.operationlog.common;

public final class ErrorCode {
    public static final int UNAUTHORIZED = 1001;
    public static final int PERMISSION_DENIED = 1003;
    public static final int PARAM_FORMAT = 1101;
    public static final int OPERATION_LOG_NOT_FOUND = 8101;
    public static final int INTERNAL_ERROR = 9999;

    private ErrorCode() {
    }
}
