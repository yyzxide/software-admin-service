package com.xcappstore.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.auth.rbac.AdminPermissionEntity;
import com.xcappstore.admin.auth.rbac.AdminRbacMapper;
import com.xcappstore.admin.auth.rbac.AdminRoleEntity;
import com.xcappstore.admin.auth.rbac.AdminUserEntity;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminTokenServiceTest {
    @Test
    void issuesAndVerifiesToken() {
        AdminTokenService service = new AdminTokenService(1L, "admin", "admin123456", "", "test-secret", 7200L);
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");

        LoginResponse response = service.login(request);
        AdminPrincipal principal = service.verify(response.getAccessToken());

        assertEquals(1L, principal.getUserId());
        assertEquals("admin", principal.getUsername());
        assertEquals("admin", principal.getUserType());
    }

    @Test
    void rejectsWrongPassword() {
        AdminTokenService service = new AdminTokenService(1L, "admin", "admin123456", "", "test-secret", 7200L);
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("bad-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request));

        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
    }

    @Test
    void acceptsSha256PasswordHashWhenConfigured() {
        AdminTokenService service = new AdminTokenService(
            1L,
            "admin",
            "",
            "ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78",
            "test-secret",
            7200L
        );
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");

        LoginResponse response = service.login(request);

        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void logsInDatabaseAdminUserWhenPresent() {
        FakeRbacMapper mapper = new FakeRbacMapper();
        AdminUserEntity user = new AdminUserEntity();
        user.setId(7L);
        user.setUsername("reviewer01");
        user.setStatus(1);
        user.setPasswordSha256("ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78");
        user.setTokenVersion(0L);
        mapper.user = user;

        AdminTokenService service = new AdminTokenService(
            mapper,
            new PasswordHashService(),
            1L,
            "admin",
            "admin123456",
            "",
            "",
            "test-secret",
            7200L,
            "test"
        );
        LoginRequest request = new LoginRequest();
        request.setUsername("reviewer01");
        request.setPassword("admin123456");

        AdminPrincipal principal = service.verify(service.login(request).getAccessToken());

        assertEquals(7L, principal.getUserId());
        assertEquals("reviewer01", principal.getUsername());
        assertEquals(1L, mapper.user.getTokenVersion());
        assertEquals(true, mapper.user.getPasswordHash().startsWith("{bcrypt}"));
    }

    @Test
    void rejectsTokenAfterPasswordVersionChanges() {
        FakeRbacMapper mapper = new FakeRbacMapper();
        AdminUserEntity user = new AdminUserEntity();
        user.setId(7L);
        user.setUsername("reviewer01");
        user.setStatus(1);
        user.setPasswordHash(new PasswordHashService().hash("admin123456"));
        user.setTokenVersion(0L);
        mapper.user = user;
        AdminTokenService service = new AdminTokenService(
            mapper,
            new PasswordHashService(),
            1L,
            "admin",
            "admin123456",
            "",
            "",
            "test-secret",
            7200L,
            "test"
        );
        LoginRequest request = new LoginRequest();
        request.setUsername("reviewer01");
        request.setPassword("admin123456");
        String token = service.login(request).getAccessToken();

        mapper.user.setTokenVersion(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify(token));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    private static final class FakeRbacMapper implements AdminRbacMapper {
        private AdminUserEntity user;

        @Override public AdminUserEntity selectUserById(Long id) { return user != null && id.equals(user.getId()) ? user : null; }
        @Override public AdminUserEntity selectUserByUsername(String username) { return user != null && username.equals(user.getUsername()) ? user : null; }
        @Override public List<AdminUserEntity> selectUsers(String keyword, Integer status) { return List.of(); }
        @Override public long countUsersByUsername(String username, Long excludeId) { return 0; }
        @Override public int insertUser(AdminUserEntity user) { return 0; }
        @Override public int updateUser(AdminUserEntity user) { return 0; }
        @Override public int updateUserStatus(Long id, Integer status) { return 0; }
        @Override public int updateUserPassword(Long id, String passwordHash) { user.setPasswordHash(passwordHash); user.setTokenVersion((user.getTokenVersion() == null ? 0L : user.getTokenVersion()) + 1); return 1; }
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
