package com.xcappstore.operationlog.service;

import com.xcappstore.operationlog.common.PageResponse;
import com.xcappstore.operationlog.dto.OperationLogOptionsResponse;
import com.xcappstore.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.operationlog.dto.OperationLogResponse;
import com.xcappstore.operationlog.dto.OperationLogStatsResponse;

public interface OperationLogService {
    PageResponse<OperationLogResponse> list(OperationLogQueryRequest request);

    OperationLogResponse detail(Long id);

    OperationLogOptionsResponse options();

    OperationLogStatsResponse stats(OperationLogQueryRequest request);
}
