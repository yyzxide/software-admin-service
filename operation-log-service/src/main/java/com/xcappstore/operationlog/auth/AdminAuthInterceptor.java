package com.xcappstore.operationlog.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.operationlog.common.ApiResponse;
import com.xcappstore.operationlog.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private static final String USER_TYPE_ADMIN = "admin";

    private final ObjectMapper objectMapper;

    public AdminAuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String userId = request.getHeader("X-User-ID");
        String userType = request.getHeader("X-User-Type");

        if (isBlank(userId) || isBlank(userType)) {
            writeError(response, ErrorCode.UNAUTHORIZED, "未登录");
            return false;
        }

        if (!USER_TYPE_ADMIN.equals(userType.toLowerCase(Locale.ROOT))) {
            writeError(response, ErrorCode.PERMISSION_DENIED, "权限不足");
            return false;
        }

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
