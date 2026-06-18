package com.xcappstore.admin.auth;

import com.xcappstore.admin.auth.dto.AdminUserResponse;
import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.auth.rbac.AdminRbacMapper;
import com.xcappstore.admin.auth.rbac.AdminUserEntity;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminTokenService {
    private static final String USER_TYPE_ADMIN = "admin";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final Long adminUserId;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminPasswordSha256;
    private final String tokenSecret;
    private final long ttlSeconds;
    private final AdminRbacMapper rbacMapper;

    @Autowired
    public AdminTokenService(
        AdminRbacMapper rbacMapper,
        @Value("${admin.security.user-id:1}") Long adminUserId,
        @Value("${admin.security.username:admin}") String adminUsername,
        @Value("${admin.security.password:admin123456}") String adminPassword,
        @Value("${admin.security.password-sha256:}") String adminPasswordSha256,
        @Value("${admin.security.token-secret:change-me-local-development-secret}") String tokenSecret,
        @Value("${admin.security.ttl-seconds:7200}") long ttlSeconds
    ) {
        this.rbacMapper = rbacMapper;
        this.adminUserId = adminUserId;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminPasswordSha256 = adminPasswordSha256;
        this.tokenSecret = tokenSecret;
        this.ttlSeconds = ttlSeconds;
    }

    public AdminTokenService(
        Long adminUserId,
        String adminUsername,
        String adminPassword,
        String adminPasswordSha256,
        String tokenSecret,
        long ttlSeconds
    ) {
        this(null, adminUserId, adminUsername, adminPassword, adminPasswordSha256, tokenSecret, ttlSeconds);
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        if (rbacMapper != null) {
            AdminUserEntity user = rbacMapper.selectUserByUsername(username);
            if (user != null) {
                return loginDbUser(user, request.getPassword());
            }
        }
        if (!adminUsername.equals(username) || !passwordMatches(request.getPassword())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "账号或密码错误");
        }

        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = String.join("|", adminUserId.toString(), adminUsername, USER_TYPE_ADMIN, Long.toString(expiresAt));
        String accessToken = encode(payload) + "." + sign(payload);
        return new LoginResponse(accessToken, "Bearer", ttlSeconds, expiresAt, toUser(new AdminPrincipal(
            adminUserId,
            adminUsername,
            USER_TYPE_ADMIN,
            expiresAt
        )));
    }

    private LoginResponse loginDbUser(AdminUserEntity user, String rawPassword) {
        if (!Integer.valueOf(1).equals(user.getStatus()) || !passwordHashMatches(rawPassword, user.getPasswordSha256())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "账号或密码错误");
        }
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = String.join("|", user.getId().toString(), user.getUsername(), USER_TYPE_ADMIN, Long.toString(expiresAt));
        String accessToken = encode(payload) + "." + sign(payload);
        return new LoginResponse(accessToken, "Bearer", ttlSeconds, expiresAt, toUser(new AdminPrincipal(
            user.getId(),
            user.getUsername(),
            USER_TYPE_ADMIN,
            expiresAt
        )));
    }

    public AdminPrincipal verify(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }

        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }

        String payload = decode(parts[0]);
        if (!sign(payload).equals(parts[1])) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }

        String[] fields = payload.split("\\|", -1);
        if (fields.length != 4) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }

        long expiresAt = Long.parseLong(fields[3]);
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }

        return new AdminPrincipal(Long.parseLong(fields[0]), fields[1], fields[2], expiresAt);
    }

    public AdminUserResponse toUser(AdminPrincipal principal) {
        return new AdminUserResponse(principal.getUserId(), principal.getUsername(), principal.getUserType(), principal.getExpiresAt());
    }

    private boolean passwordMatches(String rawPassword) {
        String password = rawPassword == null ? "" : rawPassword;
        if (StringUtils.hasText(adminPasswordSha256)) {
            String expected = adminPasswordSha256.trim().toLowerCase(Locale.ROOT);
            return MessageDigest.isEqual(sha256Hex(password).getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
        }
        return adminPassword.equals(password);
    }

    private boolean passwordHashMatches(String rawPassword, String expectedSha256) {
        if (!StringUtils.hasText(expectedSha256)) {
            return false;
        }
        String expected = expectedSha256.trim().toLowerCase(Locale.ROOT);
        return MessageDigest.isEqual(sha256Hex(rawPassword == null ? "" : rawPassword).getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "密码校验失败");
        }
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录签名失败");
        }
    }
}
