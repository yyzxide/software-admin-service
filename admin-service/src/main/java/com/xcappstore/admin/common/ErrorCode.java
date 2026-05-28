package com.xcappstore.admin.common;

public final class ErrorCode {
    public static final int UNAUTHORIZED = 1001;
    public static final int PERMISSION_DENIED = 1003;
    public static final int PARAM_FORMAT = 1101;
    public static final int RESOURCE_NOT_FOUND = 1404;
    public static final int DUPLICATE_RESOURCE = 1409;
    public static final int INVALID_STATUS_FLOW = 1422;
    public static final int CATEGORY_NOT_FOUND = 2101;
    public static final int CATEGORY_NAME_EXISTS = 2102;
    public static final int CATEGORY_DEPTH_EXCEEDED = 2103;
    public static final int CATEGORY_HAS_CHILDREN = 2104;
    public static final int CATEGORY_HAS_APPS = 2105;
    public static final int TAG_NOT_FOUND = 2201;
    public static final int TAG_NAME_EXISTS = 2202;
    public static final int TAG_HAS_APPS = 2203;
    public static final int SOFTWARE_NOT_FOUND = 2301;
    public static final int SOFTWARE_INVALID_STATUS = 2302;
    public static final int INTERNAL_ERROR = 9999;

    private ErrorCode() {
    }
}
