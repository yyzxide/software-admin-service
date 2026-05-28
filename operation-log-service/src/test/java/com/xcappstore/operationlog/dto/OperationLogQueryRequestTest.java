package com.xcappstore.operationlog.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

class OperationLogQueryRequestTest {

    @Test
    void supportsSnakeCaseQueryAliases() {
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        BeanWrapper wrapper = new BeanWrapperImpl(request);

        wrapper.setPropertyValue("user_type", "admin");
        wrapper.setPropertyValue("detail_keyword", "publish");
        wrapper.setPropertyValue("resource_id", 123L);
        wrapper.setPropertyValue("start_time", "2026-05-01");
        wrapper.setPropertyValue("end_time", "2026-05-26");
        wrapper.setPropertyValue("page_size", 50);

        assertEquals("admin", request.getUserType());
        assertEquals("publish", request.getDetailKeyword());
        assertEquals(123L, request.getResourceId());
        assertEquals("2026-05-01", request.getStartTime());
        assertEquals("2026-05-26", request.getEndTime());
        assertEquals(50, request.getPageSize());
    }
}
