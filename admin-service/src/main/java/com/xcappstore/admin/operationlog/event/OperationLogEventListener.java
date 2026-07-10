package com.xcappstore.admin.operationlog.event;

import com.xcappstore.admin.operationlog.service.OperationLogService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OperationLogEventListener {
    private final OperationLogService operationLogService;

    public OperationLogEventListener(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOperationLog(OperationLogEvent event) {
        operationLogService.record(event.command());
    }
}
