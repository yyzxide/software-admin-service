package com.xcappstore.admin.auth;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordHashService {
    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final String SHA256_PREFIX = "{sha256}";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return BCRYPT_PREFIX + encoder.encode(rawPassword == null ? "" : rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash, String legacySha256) {
        String password = rawPassword == null ? "" : rawPassword;
        if (StringUtils.hasText(passwordHash)) {
            String normalizedHash = passwordHash.trim();
            if (normalizedHash.startsWith(BCRYPT_PREFIX)) {
                return encoder.matches(password, normalizedHash.substring(BCRYPT_PREFIX.length()));
            }
            if (normalizedHash.startsWith(SHA256_PREFIX)) {
                return sha256Matches(password, normalizedHash.substring(SHA256_PREFIX.length()));
            }
            if (normalizedHash.startsWith("$2a$") || normalizedHash.startsWith("$2b$") || normalizedHash.startsWith("$2y$")) {
                return encoder.matches(password, normalizedHash);
            }
        }
        return sha256Matches(password, legacySha256);
    }

    public boolean needsUpgrade(String passwordHash) {
        return !StringUtils.hasText(passwordHash) || !passwordHash.trim().startsWith(BCRYPT_PREFIX);
    }

    private boolean sha256Matches(String rawPassword, String expectedSha256) {
        if (!StringUtils.hasText(expectedSha256)) {
            return false;
        }
        String expected = expectedSha256.trim().toLowerCase(Locale.ROOT);
        return MessageDigest.isEqual(
            sha256Hex(rawPassword).getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "密码校验失败");
        }
    }
}
