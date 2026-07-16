package com.xcappstore.admin.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.auth.rbac.AdminPermissionEntity;
import com.xcappstore.admin.auth.rbac.AdminRbacMapper;
import com.xcappstore.admin.auth.rbac.AdminRoleEntity;
import com.xcappstore.admin.auth.rbac.AdminUserEntity;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminTokenServiceTest {
    private static final String TEST_SECRET = "test-secret-at-least-32-bytes-long";

    @Test
    void issuesAndVerifiesToken() {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");

        LoginResponse response = service.login(request);
        AdminPrincipal principal = service.verify(response.getAccessToken());

        assertEquals(3, response.getAccessToken().split("\\.", -1).length);
        assertEquals(1L, principal.getUserId());
        assertEquals("admin", principal.getUsername());
        assertEquals("admin", principal.getUserType());
    }

    @Test
    void issuesStandardJwtClaims() throws Exception {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");

        SignedJWT jwt = SignedJWT.parse(service.login(request).getAccessToken());
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertEquals(JWSAlgorithm.HS256, jwt.getHeader().getAlgorithm());
        assertEquals(JOSEObjectType.JWT, jwt.getHeader().getType());
        assertEquals("xcappstore-admin", claims.getIssuer());
        assertEquals("1", claims.getSubject());
        assertEquals("admin", claims.getStringClaim("username"));
        assertEquals("admin", claims.getStringClaim("user_type"));
        assertEquals(0L, claims.getLongClaim("token_version"));
        assertTrue(claims.getJWTID() != null && !claims.getJWTID().isBlank());
        assertTrue(claims.getExpirationTime().after(claims.getIssueTime()));
    }

    @Test
    void rejectsWrongPassword() {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("bad-password");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void acceptsSha256PasswordHashWhenConfigured() {
        FakeRbacMapper mapper = new FakeRbacMapper();
        AdminUserEntity user = user(1L, "admin");
        user.setPasswordHash("");
        user.setPasswordSha256("ac0e7d037817094e9e0b4441f9bae3209d67b02fa484917065f71b16109a1a78");
        mapper.user = user;
        AdminTokenService service = new AdminTokenService(mapper, new PasswordHashService(), TEST_SECRET, 7200L, "test");
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
            TEST_SECRET,
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
            TEST_SECRET,
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

    @Test
    void rejectsMalformedJwtAsUnauthorized() {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify("not.a.jwt"));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void rejectsTamperedJwt() {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123456");
        String token = service.login(request).getAccessToken();
        String[] parts = token.split("\\.", -1);
        String tampered = parts[0] + "." + parts[1] + "." + mutate(parts[2]);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify(tampered));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void rejectsExpiredJwt() throws Exception {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");
        String token = expiredJwt();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.verify(token));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    @Test
    void rejectsWeakHs256Secret() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new AdminTokenService(
            new FakeRbacMapper(),
            new PasswordHashService(),
            "too-short",
            7200L,
            "test"
        ));

        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    void rejectsUnknownUsernameWithoutConfigFallback() {
        AdminTokenService service = serviceWithUser(1L, "admin", "admin123456");
        LoginRequest request = new LoginRequest();
        request.setUsername("missing-admin");
        request.setPassword("admin123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.login(request));

        assertEquals(ErrorCode.UNAUTHORIZED, ex.getCode());
    }

    private AdminTokenService serviceWithUser(Long id, String username, String rawPassword) {
        FakeRbacMapper mapper = new FakeRbacMapper();
        mapper.user = user(id, username);
        mapper.user.setPasswordHash(new PasswordHashService().hash(rawPassword));
        return new AdminTokenService(mapper, new PasswordHashService(), TEST_SECRET, 7200L, "test");
    }

    private AdminUserEntity user(Long id, String username) {
        AdminUserEntity user = new AdminUserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(1);
        user.setTokenVersion(0L);
        return user;
    }

    private String mutate(String value) {
        char replacement = value.charAt(value.length() - 1) == 'A' ? 'B' : 'A';
        return value.substring(0, value.length() - 1) + replacement;
    }

    private String expiredJwt() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer("xcappstore-admin")
            .subject("1")
            .issueTime(Date.from(now.minusSeconds(180)))
            .expirationTime(Date.from(now.minusSeconds(120)))
            .jwtID("expired-token")
            .claim("username", "admin")
            .claim("user_type", "admin")
            .claim("token_version", 0L)
            .build();
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
            claims
        );
        jwt.sign(new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
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
