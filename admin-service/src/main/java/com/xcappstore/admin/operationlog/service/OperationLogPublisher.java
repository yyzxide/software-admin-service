package com.xcappstore.admin.operationlog.service;

import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.event.OperationLogEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OperationLogPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public OperationLogPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void record(OperationLogCreateCommand command) {
        eventPublisher.publishEvent(new OperationLogEvent(command));
    }

    public void record(Long userId, String action, String resourceType, Long resourceId, String resourceName, String content) {
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
