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
import java.util.UUID;
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

    private final String tokenSecret;
    private final long ttlSeconds;
    private final AdminRbacMapper rbacMapper;
    private final PasswordHashService passwordHashService;

    @Autowired
    public AdminTokenService(
        AdminRbacMapper rbacMapper,
        PasswordHashService passwordHashService,
        @Value("${admin.security.token-secret:change-me-local-development-secret}") String tokenSecret,
        @Value("${admin.security.ttl-seconds:7200}") long ttlSeconds,
        @Value("${spring.profiles.active:}") String activeProfiles
    ) {
        this.rbacMapper = rbacMapper;
        this.passwordHashService = passwordHashService;
        this.tokenSecret = tokenSecret;
        this.ttlSeconds = ttlSeconds;
        rejectDefaultSecretOutsideLocalProfile(activeProfiles);
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        AdminUserEntity user = rbacMapper == null ? null : rbacMapper.selectUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "账号或密码错误");
        }
        return loginDbUser(user, request.getPassword());
    }

    private LoginResponse loginDbUser(AdminUserEntity user, String rawPassword) {
        if (!Integer.valueOf(1).equals(user.getStatus())
            || !passwordHashService.matches(rawPassword, user.getPasswordHash(), user.getPasswordSha256())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "账号或密码错误");
        }
        long tokenVersion = normalizeTokenVersion(user.getTokenVersion());
        if (passwordHashService.needsUpgrade(user.getPasswordHash()) && rbacMapper != null) {
            rbacMapper.updateUserPassword(user.getId(), passwordHashService.hash(rawPassword));
            tokenVersion++;
        }
        return issueToken(user.getId(), user.getUsername(), USER_TYPE_ADMIN, tokenVersion);
    }

    private LoginResponse issueToken(Long userId, String username, String userType, Long tokenVersion) {
        long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = String.join(
            "|",
            userId.toString(),
            username,
            userType,
            Long.toString(expiresAt),
            Long.toString(normalizeTokenVersion(tokenVersion)),
            UUID.randomUUID().toString()
        );
        String accessToken = encode(payload) + "." + sign(payload);
        return new LoginResponse(accessToken, "Bearer", ttlSeconds, expiresAt, toUser(new AdminPrincipal(
            userId,
            username,
            userType,
            expiresAt
        )));
    }

    public AdminPrincipal verify(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }

        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }

            String payload = decode(parts[0]);
            if (!signatureMatches(sign(payload), parts[1])) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }

            String[] fields = payload.split("\\|", -1);
            if (fields.length != 4 && fields.length != 6) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }

            long userId = Long.parseLong(fields[0]);
            String username = fields[1];
            String userType = fields[2];
            long expiresAt = Long.parseLong(fields[3]);
            long tokenVersion = fields.length == 6 ? Long.parseLong(fields[4]) : 0L;
            if (Instant.now().getEpochSecond() > expiresAt) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }
            verifyDbPrincipal(userId, username, userType, tokenVersion, fields.length == 6);
            return new AdminPrincipal(userId, username, userType, expiresAt);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
    }

    public AdminUserResponse toUser(AdminPrincipal principal) {
        return new AdminUserResponse(principal.getUserId(), principal.getUsername(), principal.getUserType(), principal.getExpiresAt());
    }

    private void verifyDbPrincipal(Long userId, String username, String userType, long tokenVersion, boolean versionedToken) {
        if (rbacMapper == null || !USER_TYPE_ADMIN.equals(userType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
        AdminUserEntity user = rbacMapper.selectUserById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || !username.equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
        long currentVersion = normalizeTokenVersion(user.getTokenVersion());
        if ((versionedToken && tokenVersion != currentVersion) || (!versionedToken && currentVersion > 0)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
    }

    private long normalizeTokenVersion(Long tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0L : tokenVersion;
    }

    private void rejectDefaultSecretOutsideLocalProfile(String activeProfiles) {
        if (!"change-me-local-development-secret".equals(tokenSecret)) {
            return;
        }
        String profiles = activeProfiles == null ? "" : activeProfiles.toLowerCase();
        if (profiles.contains("prod") || profiles.contains("stage") || profiles.contains("release")) {
            throw new IllegalStateException("非本地环境必须配置 ADMIN_SECURITY_TOKEN_SECRET");
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

    private boolean signatureMatches(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            (actual == null ? "" : actual).getBytes(StandardCharsets.UTF_8)
        );
    }
}
