package com.xcappstore.admin.operationlog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.admin.operationlog.dto.OperationLogResponse;
import com.xcappstore.admin.operationlog.dto.StatItem;
import com.xcappstore.admin.operationlog.entity.OperationLogEntity;
import com.xcappstore.admin.operationlog.mapper.OperationLogMapper;
import com.xcappstore.admin.operationlog.service.impl.OperationLogServiceImpl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationLogServiceImplTest {
    @Test
    void recordsOperationLogWithStructuredDetail() {
        FakeOperationLogMapper mapper = new FakeOperationLogMapper();
        OperationLogService service = new OperationLogServiceImpl(mapper, new ObjectMapper());
        OperationLogCreateCommand command = new OperationLogCreateCommand();
        command.setUserId(99L);
        command.setAction("software_publish");
        command.setResourceType("software");
        command.setResourceId(1L);
        command.setResourceName("编辑器");
        command.setContent("上架软件: 编辑器");

        service.record(command);

        assertEquals(1, mapper.logs.size());
        assertEquals("software_publish", mapper.logs.get(0).getAction());
        assertEquals("编辑器", mapper.logs.get(0).getResourceName());
    }

    @Test
    void rejectsInvalidTimeRange() {
        OperationLogService service = new OperationLogServiceImpl(new FakeOperationLogMapper(), new ObjectMapper());
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setStartTime("bad-time");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(request));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
    }

    @Test
    void normalizesInclusiveEndDateToExclusiveNextDay() {
        OperationLogService service = new OperationLogServiceImpl(new FakeOperationLogMapper(), new ObjectMapper());
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setEndTime("2026-07-10");

        service.list(request);

        assertEquals("2026-07-11 00:00:00", request.getEndTime());
    }

    @Test
    void rejectsReversedTimeRange() {
        OperationLogService service = new OperationLogServiceImpl(new FakeOperationLogMapper(), new ObjectMapper());
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setStartTime("2026-07-11 00:00:00");
        request.setEndTime("2026-07-10 23:59:59");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(request));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("开始时间不能晚于结束时间", ex.getMessage());
    }

    @Test
    void rejectsOverflowingEndDate() {
        OperationLogService service = new OperationLogServiceImpl(new FakeOperationLogMapper(), new ObjectMapper());
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setEndTime("9999-12-31");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.list(request));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("结束时间格式错误", ex.getMessage());
    }

    @Test
    void returnsPagedOperationLogs() {
        FakeOperationLogMapper mapper = new FakeOperationLogMapper();
        OperationLogEntity entity = new OperationLogEntity();
        entity.setId(1L);
        entity.setUserId(99L);
        entity.setUsername("admin");
        entity.setUserType("admin");
        entity.setAction("software_upload");
        entity.setResourceType("software");
        entity.setResourceName("编辑器");
        entity.setDetail("{\"content\":\"上传软件: 编辑器\"}");
        entity.setCreatedAt(LocalDateTime.now());
        mapper.logs.add(entity);
        OperationLogService service = new OperationLogServiceImpl(mapper, new ObjectMapper());

        PageResponse<OperationLogResponse> page = service.list(new OperationLogQueryRequest());

        assertEquals(1, page.getTotal());
        assertEquals("上传软件: 编辑器", page.getList().get(0).getDisplayDetail());
    }

    private static final class FakeOperationLogMapper implements OperationLogMapper {
        private final List<OperationLogEntity> logs = new ArrayList<>();

        @Override
        public int insert(OperationLogEntity entity) {
            entity.setId((long) logs.size() + 1);
            logs.add(entity);
            return 1;
        }

        @Override public long count(OperationLogQueryRequest query) { return logs.size(); }
        @Override public List<OperationLogEntity> selectPage(OperationLogQueryRequest query, int offset, int limit) { return logs; }
        @Override public OperationLogEntity selectById(Long id) { return logs.stream().filter(log -> id.equals(log.getId())).findFirst().orElse(null); }
        @Override public List<String> selectActions() { return List.of(); }
        @Override public List<String> selectResourceTypes() { return List.of(); }
        @Override public List<StatItem> countByAction(OperationLogQueryRequest query) { return List.of(); }
        @Override public List<StatItem> countByResourceType(OperationLogQueryRequest query) { return List.of(); }
        @Override public List<StatItem> countByDate(OperationLogQueryRequest query) { return List.of(); }
    }
}
