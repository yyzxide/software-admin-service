package com.xcappstore.admin.auth;

import com.xcappstore.admin.auth.dto.AdminUserResponse;
import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
    private final String tokenSecret;
    private final long ttlSeconds;

    public AdminTokenService(
        @Value("${admin.security.user-id:1}") Long adminUserId,
        @Value("${admin.security.username:admin}") String adminUsername,
        @Value("${admin.security.password:admin123456}") String adminPassword,
        @Value("${admin.security.token-secret:xcappstore-admin-local-secret}") String tokenSecret,
        @Value("${admin.security.ttl-seconds:7200}") long ttlSeconds
    ) {
        this.adminUserId = adminUserId;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.tokenSecret = tokenSecret;
        this.ttlSeconds = ttlSeconds;
    }

    public LoginResponse login(LoginRequest request) {
        if (!adminUsername.equals(request.getUsername()) || !adminPassword.equals(request.getPassword())) {
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
