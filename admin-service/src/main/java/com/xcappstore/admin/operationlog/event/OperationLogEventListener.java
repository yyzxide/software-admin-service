package com.xcappstore.admin.operationlog.event;

import com.xcappstore.admin.operationlog.service.OperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationLogEventListener {
    private static final Logger log = LoggerFactory.getLogger(OperationLogEventListener.class);
    private final OperationLogService operationLogService;

    public OperationLogEventListener(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        try {
            operationLogService.record(event.command());
        } catch (RuntimeException ex) {
            log.error(
                "Operation log persistence failed after business transaction committed: action={}, resourceType={}, resourceId={}",
                event.command().getAction(),
                event.command().getResourceType(),
                event.command().getResourceId(),
                ex
            );
        }
    }
}
