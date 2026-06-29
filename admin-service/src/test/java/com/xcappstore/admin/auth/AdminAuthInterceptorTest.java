package com.xcappstore.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.auth.rbac.AdminPermissionMapper;
import com.xcappstore.admin.auth.rbac.AdminPermissionService;
import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.common.ErrorCode;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class AdminAuthInterceptorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsUnauthorizedHttpStatusWhenTokenMissing() throws Exception {
        AdminAuthInterceptor interceptor = interceptor(List.of("*"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/software/apps");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        assertEquals(ErrorCode.UNAUTHORIZED, errorCode(response));
    }

    @Test
    void returnsForbiddenHttpStatusWhenPermissionMissing() throws Exception {
        AdminTokenService tokenService = new AdminTokenService(1L, "admin", "admin123456", "", "test-secret", 7200L);
        AdminAuthInterceptor interceptor = interceptor(tokenService, List.of("software:view"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/software/apps");
        request.addHeader("Authorization", "Bearer " + tokenService.login(loginRequest()).getAccessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, securedHandler());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
        assertEquals(ErrorCode.PERMISSION_DENIED, errorCode(response));
    }

    private AdminAuthInterceptor interceptor(List<String> permissions) {
        return interceptor(new AdminTokenService(1L, "admin", "admin123456", "", "test-secret", 7200L), permissions);
    }

    private AdminAuthInterceptor interceptor(AdminTokenService tokenService, List<String> permissions) {
        AdminPermissionMapper mapper = userId -> userId.equals(1L) ? permissions : List.of();
        return new AdminAuthInterceptor(objectMapper, tokenService, new AdminPermissionService(mapper));
    }

    private com.xcappstore.admin.auth.dto.LoginRequest loginRequest() {
        com.xcappstore.admin.auth.dto.LoginRequest request = new com.xcappstore.admin.auth.dto.LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");
        return request;
    }

    private HandlerMethod securedHandler() throws NoSuchMethodException {
        SecuredController controller = new SecuredController();
        Method method = SecuredController.class.getDeclaredMethod("create");
        return new HandlerMethod(controller, method);
    }

    private int errorCode(MockHttpServletResponse response) throws Exception {
        return objectMapper.readTree(response.getContentAsByteArray()).get("code").asInt();
    }

    private static final class SecuredController {
        @RequirePermission("software:create")
        public void create() {
        }
    }
}
