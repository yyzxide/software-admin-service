package com.xcappstore.admin.operationlog.service;

import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.dto.OperationLogOptionsResponse;
import com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.admin.operationlog.dto.OperationLogResponse;
import com.xcappstore.admin.operationlog.dto.OperationLogStatsResponse;

public interface OperationLogService {
    PageResponse<OperationLogResponse> list(OperationLogQueryRequest request);

    OperationLogResponse detail(Long id);

    OperationLogOptionsResponse options();

    OperationLogStatsResponse stats(OperationLogQueryRequest request);

    void record(OperationLogCreateCommand command);

    default void record(Long userId, String action, String resourceType, Long resourceId, String resourceName, String content) {
        OperationLogCreateCommand command = new OperationLogCreateCommand();
        command.setUserId(userId);
        command.setAction(action);
        command.setResourceType(resourceType);
        command.setResourceId(resourceId);
        command.setResourceName(resourceName);
        command.setContent(content);
        record(command);
    }
}
