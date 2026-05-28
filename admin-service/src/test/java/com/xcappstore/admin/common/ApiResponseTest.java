package com.xcappstore.admin.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ApiResponseTest {
    @Test
    void createsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertEquals(0, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("ok", response.getData());
    }

    @Test
    void createsErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.PARAM_FORMAT, "参数格式错误");

        assertEquals(ErrorCode.PARAM_FORMAT, response.getCode());
        assertEquals("参数格式错误", response.getMessage());
        assertNull(response.getData());
    }
}
