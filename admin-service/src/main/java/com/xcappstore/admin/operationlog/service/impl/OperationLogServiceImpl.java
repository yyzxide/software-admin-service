package com.xcappstore.admin.operationlog.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.operationlog.dto.OperationLogCreateCommand;
import com.xcappstore.admin.operationlog.dto.OperationLogOptionsResponse;
import com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.admin.operationlog.dto.OperationLogResponse;
import com.xcappstore.admin.operationlog.dto.OperationLogStatsResponse;
import com.xcappstore.admin.operationlog.entity.OperationLogEntity;
import com.xcappstore.admin.operationlog.mapper.OperationLogMapper;
import com.xcappstore.admin.operationlog.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogServiceImpl implements OperationLogService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String USER_TYPE_ADMIN = "admin";

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResponse<OperationLogResponse> list(OperationLogQueryRequest request) {
        normalizeQuery(request);
        long total = operationLogMapper.count(request);
        List<OperationLogResponse> list = total == 0 ? List.of() : operationLogMapper
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
        return new OperationLogOptionsResponse(operationLogMapper.selectActions(), operationLogMapper.selectResourceTypes());
    }

    @Override
    public OperationLogStatsResponse stats(OperationLogQueryRequest request) {
        normalizeQuery(request);
        return new OperationLogStatsResponse(
            operationLogMapper.countByAction(request),
            operationLogMapper.countByResourceType(request),
            operationLogMapper.countByDate(request)
        );
    }

    @Override
    public void record(OperationLogCreateCommand command) {
        OperationLogEntity entity = new OperationLogEntity();
        HttpServletRequest request = currentRequest();
        AdminPrincipal principal = currentPrincipal(request);

        Long userId = firstPositive(command.getUserId(), principal == null ? null : principal.getUserId());
        String username = firstText(command.getUsername(), principal == null ? null : principal.getUsername(), userId == null ? "system" : "admin-" + userId);
        String userType = firstText(command.getUserType(), principal == null ? null : principal.getUserType(), USER_TYPE_ADMIN);

        entity.setUserId(userId == null ? 0L : userId);
        entity.setUsername(username);
        entity.setUserType(userType);
        entity.setAction(requireText(command.getAction(), "操作类型不能为空"));
        entity.setResourceType(requireText(command.getResourceType(), "资源类型不能为空"));
        entity.setResourceId(command.getResourceId());
        entity.setResourceName(limit(firstText(command.getResourceName(), ""), 128));
        entity.setDetail(toDetailJson(command.getContent()));
        entity.setIp(request == null ? "" : clientIp(request));
        entity.setUserAgent(request == null ? "" : limit(firstText(request.getHeader("User-Agent"), ""), 512));
        entity.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    private void normalizeQuery(OperationLogQueryRequest request) {
        LocalDateTime startTime = null;
        LocalDateTime endTimeExclusive = null;
        if (StringUtils.hasText(request.getStartTime())) {
            startTime = parseStartTime(request.getStartTime());
            request.setStartTime(startTime.format(DATE_TIME_FORMATTER));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            endTimeExclusive = parseEndTimeExclusive(request.getEndTime());
            request.setEndTime(endTimeExclusive.format(DATE_TIME_FORMATTER));
        }
        if (startTime != null && endTimeExclusive != null && !startTime.isBefore(endTimeExclusive)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "开始时间不能晚于结束时间");
        }
    }

    private LocalDateTime parseStartTime(String value) {
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                return requireMySqlDateTime(LocalDate.parse(trimmed, DATE_FORMATTER).atStartOfDay());
            }
            return requireMySqlDateTime(LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER));
        } catch (DateTimeException ex) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "开始时间格式错误");
        }
    }

    private LocalDateTime parseEndTimeExclusive(String value) {
        String trimmed = value.trim();
        try {
            if (trimmed.length() == 10) {
                return requireMySqlDateTime(LocalDate.parse(trimmed, DATE_FORMATTER).plusDays(1).atStartOfDay());
            }
            return requireMySqlDateTime(LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER).plusSeconds(1));
        } catch (DateTimeException ex) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "结束时间格式错误");
        }
    }

    private LocalDateTime requireMySqlDateTime(LocalDateTime value) {
        if (value.getYear() < 1000 || value.getYear() > 9999) {
            throw new DateTimeException("time is outside MySQL DATETIME range");
        }
        return value;
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

    private String toDetailJson(String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", firstText(content, ""));
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "操作日志内容序列化失败");
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private AdminPrincipal currentPrincipal(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object principal = request.getAttribute(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR);
        return principal instanceof AdminPrincipal adminPrincipal ? adminPrincipal : null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return limit(forwarded.split(",")[0].trim(), 64);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return limit(realIp.trim(), 64);
        }
        return limit(firstText(request.getRemoteAddr(), ""), 64);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, message);
        }
        return value.trim();
    }

    private Long firstPositive(Long first, Long second) {
        if (first != null && first > 0) {
            return first;
        }
        if (second != null && second > 0) {
            return second;
        }
        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
