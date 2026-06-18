package com.xcappstore.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.auth.rbac.AdminRbacMapper;
import com.xcappstore.admin.auth.rbac.AdminUserEntity;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        AdminRbacMapper mapper = Mockito.mock(AdminRbacMapper.class);
        AdminUserEntity user = new AdminUserEntity();
        user.setId(7L);
        user.setUsername("reviewer01");
        user.setStatus(1);
        user.setPasswordSha256("ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78");
        when(mapper.selectUserByUsername("reviewer01")).thenReturn(user);

        AdminTokenService service = new AdminTokenService(
            mapper,
            1L,
            "admin",
            "admin123456",
            "",
            "test-secret",
            7200L
        );
        LoginRequest request = new LoginRequest();
        request.setUsername("reviewer01");
        request.setPassword("admin123456");

        AdminPrincipal principal = service.verify(service.login(request).getAccessToken());

        assertEquals(7L, principal.getUserId());
        assertEquals("reviewer01", principal.getUsername());
    }
}
