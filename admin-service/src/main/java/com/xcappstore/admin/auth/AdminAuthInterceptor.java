package com.xcappstore.admin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.auth.rbac.AdminPermissionService;
import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.common.ApiResponse;
import com.xcappstore.admin.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    public static final String ADMIN_PRINCIPAL_ATTR = "adminPrincipal";

    private final ObjectMapper objectMapper;
    private final AdminTokenService adminTokenService;
    private final AdminPermissionService adminPermissionService;

    public AdminAuthInterceptor(
        ObjectMapper objectMapper,
        AdminTokenService adminTokenService,
        AdminPermissionService adminPermissionService
    ) {
        this.objectMapper = objectMapper;
        this.adminTokenService = adminTokenService;
        this.adminPermissionService = adminPermissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        AdminPrincipal tokenPrincipal = resolveBearerPrincipal(request);
        if (tokenPrincipal != null) {
            request.setAttribute(ADMIN_PRINCIPAL_ATTR, tokenPrincipal);
            return checkPermission(request, response, handler, tokenPrincipal);
        }

        writeError(response, HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED, "未登录");
        return false;
    }

    private boolean checkPermission(
        HttpServletRequest request,
        HttpServletResponse response,
        Object handler,
        AdminPrincipal principal
    ) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            permission = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permission == null || adminPermissionService.hasPermission(principal, permission.value())) {
            return true;
        }

        writeError(response, HttpStatus.FORBIDDEN.value(), ErrorCode.PERMISSION_DENIED, "权限不足，缺少权限: " + permission.value());
        return false;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void writeError(HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
