package com.xcappstore.admin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.common.ErrorCode;
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
    public static final String ADMIN_PRINCIPAL_ATTR = "adminPrincipal";
    private static final String USER_TYPE_ADMIN = "admin";

    private final ObjectMapper objectMapper;
    private final AdminTokenService adminTokenService;

    public AdminAuthInterceptor(ObjectMapper objectMapper, AdminTokenService adminTokenService) {
        this.objectMapper = objectMapper;
        this.adminTokenService = adminTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AdminPrincipal tokenPrincipal = resolveBearerPrincipal(request);
        if (tokenPrincipal != null) {
            request.setAttribute(ADMIN_PRINCIPAL_ATTR, tokenPrincipal);
            return true;
        }

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

        request.setAttribute(ADMIN_PRINCIPAL_ATTR, new AdminPrincipal(parseUserId(userId), "gateway-admin", USER_TYPE_ADMIN, 0L));
        return true;
    }

    private AdminPrincipal resolveBearerPrincipal(HttpServletRequest request) throws IOException {
        String authorization = request.getHeader("Authorization");
        if (isBlank(authorization) || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = authorization.substring(7).trim();
        try {
            return adminTokenService.verify(token);
        } catch (Exception ex) {
            return null;
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (Exception ex) {
            return 0L;
        }
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
