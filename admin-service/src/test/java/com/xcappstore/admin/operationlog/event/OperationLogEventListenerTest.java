package com.xcappstore.admin.operationlog.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.service.OperationLogService;
import org.junit.jupiter.api.Test;

class OperationLogEventListenerTest {
    @Test
    void doesNotFailCompletedBusinessRequestWhenAfterCommitAuditWriteFails() {
        OperationLogService service = mock(OperationLogService.class);
        OperationLogCreateCommand command = new OperationLogCreateCommand();
        command.setAction("software_publish");
        command.setResourceType("software");
        command.setResourceId(1L);
        doThrow(new IllegalStateException("database unavailable")).when(service).record(command);

        OperationLogEventListener listener = new OperationLogEventListener(service);

        assertDoesNotThrow(() -> listener.onOperationLog(new OperationLogEvent(command)));
    }
}
