package com.xcappstore.admin.auth;

import com.xcappstore.admin.auth.dto.AdminUserResponse;
import com.xcappstore.admin.auth.dto.LoginRequest;
import com.xcappstore.admin.auth.dto.LoginResponse;
import com.xcappstore.admin.auth.rbac.AdminRbacMapper;
import com.xcappstore.admin.auth.rbac.AdminUserEntity;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminTokenService {
    private static final String USER_TYPE_ADMIN = "admin";
    private static final String JWT_ISSUER = "xcappstore-admin";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_HS256_SECRET_BYTES = 32;
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_USER_TYPE = "user_type";
    private static final String CLAIM_TOKEN_VERSION = "token_version";

    private final long ttlSeconds;
    private final AdminRbacMapper rbacMapper;
    private final PasswordHashService passwordHashService;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

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
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("JWT 有效期必须大于 0 秒");
        }
        this.ttlSeconds = ttlSeconds;
        rejectDefaultSecretOutsideLocalProfile(tokenSecret, activeProfiles);
        byte[] secretBytes = tokenSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_HS256_SECRET_BYTES) {
            throw new IllegalArgumentException("JWT HS256 密钥至少需要 32 字节");
        }
        SecretKey secretKey = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey));
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JWT_ISSUER));
        this.jwtDecoder = decoder;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        AdminUserEntity user = rbacMapper == null ? null : rbacMapper.selectUserByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return loginDbUser(user, request.getPassword());
    }

    private LoginResponse loginDbUser(AdminUserEntity user, String rawPassword) {
        if (!Integer.valueOf(1).equals(user.getStatus())
            || !passwordHashService.matches(rawPassword, user.getPasswordHash(), user.getPasswordSha256())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        long tokenVersion = normalizeTokenVersion(user.getTokenVersion());
        if (passwordHashService.needsUpgrade(user.getPasswordHash()) && rbacMapper != null) {
            rbacMapper.updateUserPassword(user.getId(), passwordHashService.hash(rawPassword));
            tokenVersion++;
        }
        return issueToken(user.getId(), user.getUsername(), USER_TYPE_ADMIN, tokenVersion);
    }

    private LoginResponse issueToken(Long userId, String username, String userType, Long tokenVersion) {
        Instant issuedAt = Instant.now();
        Instant expiresAtInstant = issuedAt.plusSeconds(ttlSeconds);
        long expiresAt = expiresAtInstant.getEpochSecond();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(JWT_ISSUER)
            .subject(userId.toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAtInstant)
            .id(UUID.randomUUID().toString())
            .claim(CLAIM_USERNAME, username)
            .claim(CLAIM_USER_TYPE, userType)
            .claim(CLAIM_TOKEN_VERSION, normalizeTokenVersion(tokenVersion))
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
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
            Jwt jwt = jwtDecoder.decode(token);
            long userId = Long.parseLong(jwt.getSubject());
            String username = jwt.getClaimAsString(CLAIM_USERNAME);
            String userType = jwt.getClaimAsString(CLAIM_USER_TYPE);
            Number tokenVersionClaim = jwt.getClaim(CLAIM_TOKEN_VERSION);
            long tokenVersion = tokenVersionClaim == null ? -1L : tokenVersionClaim.longValue();
            if (jwt.getIssuedAt() == null || !StringUtils.hasText(jwt.getId())
                || !StringUtils.hasText(username) || !StringUtils.hasText(userType) || tokenVersion < 0) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }
            Instant expiresAtInstant = jwt.getExpiresAt();
            if (expiresAtInstant == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }
            long expiresAt = expiresAtInstant.getEpochSecond();
            verifyDbPrincipal(userId, username, userType, tokenVersion);
            return new AdminPrincipal(userId, username, userType, expiresAt);
        } catch (BusinessException ex) {
            throw ex;
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
    }

    public AdminUserResponse toUser(AdminPrincipal principal) {
        return new AdminUserResponse(principal.getUserId(), principal.getUsername(), principal.getUserType(), principal.getExpiresAt());
    }

    private void verifyDbPrincipal(Long userId, String username, String userType, long tokenVersion) {
        if (rbacMapper == null || !USER_TYPE_ADMIN.equals(userType)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
        AdminUserEntity user = rbacMapper.selectUserById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || !username.equals(user.getUsername())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
        long currentVersion = normalizeTokenVersion(user.getTokenVersion());
        if (tokenVersion != currentVersion) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
        }
    }

    private long normalizeTokenVersion(Long tokenVersion) {
        return tokenVersion == null || tokenVersion < 0 ? 0L : tokenVersion;
    }

    private void rejectDefaultSecretOutsideLocalProfile(String tokenSecret, String activeProfiles) {
        if (!"change-me-local-development-secret".equals(tokenSecret)) {
            return;
        }
        String profiles = activeProfiles == null ? "" : activeProfiles.toLowerCase();
        if (profiles.contains("prod") || profiles.contains("stage") || profiles.contains("release")) {
            throw new IllegalStateException("非本地环境必须配置 ADMIN_SECURITY_TOKEN_SECRET");
        }
    }

}
