package com.xcappstore.operationlog.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.operationlog.common.ErrorCode;
import com.xcappstore.operationlog.common.PageResponse;
import com.xcappstore.operationlog.dto.OperationLogOptionsResponse;
import com.xcappstore.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.operationlog.dto.OperationLogResponse;
import com.xcappstore.operationlog.dto.OperationLogStatsResponse;
import com.xcappstore.operationlog.entity.OperationLogEntity;
import com.xcappstore.operationlog.exception.BusinessException;
import com.xcappstore.operationlog.mapper.OperationLogMapper;
import com.xcappstore.operationlog.service.OperationLogService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResponse<OperationLogResponse> list(OperationLogQueryRequest request) {
        normalizeTimeRange(request);
        long total = operationLogMapper.count(request);
        List<OperationLogResponse> list = operationLogMapper
            .selectPage(request, request.offset(), request.normalizedPageSize())
            .stream()
            .map(this::toResponse)
            .toList();
        return new PageResponse<>(total, request.normalizedPage(), request.normalizedPageSize(), list);
    }

    @Override
    public OperationLogResponse detail(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "参数格式错误");
        }
        OperationLogEntity entity = operationLogMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.OPERATION_LOG_NOT_FOUND, "操作日志不存在");
        }
        return toResponse(entity);
    }

    @Override
    public OperationLogOptionsResponse options() {
        return new OperationLogOptionsResponse(
            operationLogMapper.selectActions(),
            operationLogMapper.selectResourceTypes()
        );
    }

    @Override
    public OperationLogStatsResponse stats(OperationLogQueryRequest request) {
        normalizeTimeRange(request);
        return new OperationLogStatsResponse(
            operationLogMapper.countByAction(request),
            operationLogMapper.countByResourceType(request),
            operationLogMapper.countByDate(request)
        );
    }

    private void normalizeTimeRange(OperationLogQueryRequest request) {
        if (StringUtils.hasText(request.getStartTime())) {
            request.setStartTime(parseStartTime(request.getStartTime()).format(DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            request.setEndTime(parseEndTime(request.getEndTime()).format(DATE_TIME_FORMATTER));
        }
    }

    private LocalDateTime parseStartTime(String value) {
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed, DATE_FORMATTER).atStartOfDay();
            }
            return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "开始时间格式错误");
        }
    }

    private LocalDateTime parseEndTime(String value) {
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                return LocalDate.parse(trimmed, DATE_FORMATTER).atTime(23, 59, 59);
            }
            return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "结束时间格式错误");
        }
    }

    private OperationLogResponse toResponse(OperationLogEntity entity) {
        OperationLogResponse response = new OperationLogResponse();
        response.setId(entity.getId());
        response.setUserId(entity.getUserId());
        response.setUserType(entity.getUserType());
        response.setUsername(entity.getUsername());
        response.setAction(entity.getAction());
        response.setResourceType(entity.getResourceType());
        response.setResourceId(entity.getResourceId());
        response.setResourceName(entity.getResourceName());
        response.setDetail(entity.getDetail());
        response.setDisplayDetail(extractDisplayDetail(entity.getDetail()));
        response.setIp(entity.getIp());
        response.setUserAgent(entity.getUserAgent());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedAtStr(entity.getCreatedAt() == null ? "" : entity.getCreatedAt().format(DATE_TIME_FORMATTER));
        return response;
    }

    private String extractDisplayDetail(String detail) {
        if (!StringUtils.hasText(detail)) {
            return "";
        }
        try {
            Map<String, Object> data = objectMapper.readValue(detail, new TypeReference<>() {});
            Object content = data.get("content");
            if (content instanceof String text && StringUtils.hasText(text)) {
                return text;
            }
        } catch (Exception ignored) {
            return detail;
        }
        return detail;
    }
}
