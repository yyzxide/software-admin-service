package com.xcappstore.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import org.junit.jupiter.api.Test;

class AdminTokenServiceTest {
    @Test
    void issuesAndVerifiesToken() {
        AdminTokenService service = new AdminTokenService(1L, "admin", "admin123456", "test-secret", 7200L);
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
        AdminTokenService service = new AdminTokenService(1L, "admin", "admin123456", "test-secret", 7200L);
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("bad-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request));

        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
    }
}
