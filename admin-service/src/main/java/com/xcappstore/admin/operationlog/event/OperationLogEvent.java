package com.xcappstore.admin.operationlog.event;

import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;

public record OperationLogEvent(OperationLogCreateCommand command) {
}
