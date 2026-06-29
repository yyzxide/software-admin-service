package com.xcappstore.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.auth.rbac.AdminPermissionMapper;
import com.xcappstore.admin.auth.rbac.AdminPermissionService;
import com.xcappstore.admin.auth.rbac.RequirePermission;
import com.xcappstore.admin.auth.rbac.AdminRbacMapper;
import com.xcappstore.admin.auth.rbac.AdminUserEntity;
import com.xcappstore.admin.auth.rbac.AdminRoleEntity;
import com.xcappstore.admin.auth.rbac.AdminPermissionEntity;
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
        AdminTokenService tokenService = tokenService();
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
        return interceptor(tokenService(), permissions);
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

    private AdminTokenService tokenService() {
        return new AdminTokenService(new FakeRbacMapper(), new PasswordHashService(), "test-secret", 7200L, "test");
    }

    private static final class SecuredController {
        @RequirePermission("software:create")
        public void create() {
        }
    }

    private static final class FakeRbacMapper implements AdminRbacMapper {
        private final AdminUserEntity user;

        private FakeRbacMapper() {
            this.user = new AdminUserEntity();
            this.user.setId(1L);
            this.user.setUsername("admin");
            this.user.setStatus(1);
            this.user.setPasswordHash(new PasswordHashService().hash("admin123456"));
            this.user.setTokenVersion(0L);
        }

        @Override public AdminUserEntity selectUserById(Long id) { return user.getId().equals(id) ? user : null; }
        @Override public AdminUserEntity selectUserByUsername(String username) { return user.getUsername().equals(username) ? user : null; }
        @Override public List<AdminUserEntity> selectUsers(String keyword, Integer status) { return List.of(); }
        @Override public long countUsersByUsername(String username, Long excludeId) { return 0; }
        @Override public int insertUser(AdminUserEntity user) { return 0; }
        @Override public int updateUser(AdminUserEntity user) { return 0; }
        @Override public int updateUserStatus(Long id, Integer status) { return 0; }
        @Override public int updateUserPassword(Long id, String passwordHash) { return 0; }
        @Override public List<AdminRoleEntity> selectRoles(String keyword, Integer status) { return List.of(); }
        @Override public AdminRoleEntity selectRoleById(Long id) { return null; }
        @Override public long countRolesByCode(String roleCode, Long excludeId) { return 0; }
        @Override public long countRolesByIds(List<Long> roleIds) { return 0; }
        @Override public int insertRole(AdminRoleEntity role) { return 0; }
        @Override public int updateRole(AdminRoleEntity role) { return 0; }
        @Override public List<AdminPermissionEntity> selectPermissions(String module, Integer status) { return List.of(); }
        @Override public long countPermissionsByIds(List<Long> permissionIds) { return 0; }
        @Override public List<AdminRoleEntity> selectRolesByUserId(Long userId) { return List.of(); }
        @Override public List<AdminPermissionEntity> selectPermissionsByRoleId(Long roleId) { return List.of(); }
        @Override public int deleteUserRoles(Long userId) { return 0; }
        @Override public int insertUserRole(Long userId, Long roleId) { return 0; }
        @Override public int deleteRolePermissions(Long roleId) { return 0; }
        @Override public int insertRolePermission(Long roleId, Long permissionId) { return 0; }
    }
}
