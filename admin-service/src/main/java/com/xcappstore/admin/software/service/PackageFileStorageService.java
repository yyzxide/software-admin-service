package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PackageFileStorageService {
    private static final Logger log = LoggerFactory.getLogger(PackageFileStorageService.class);
    private static final Map<String, Set<String>> FORMAT_EXTENSIONS = Map.of(
        "deb", Set.of("deb"),
        "rpm", Set.of("rpm"),
        "appimage", Set.of("appimage")
    );

    private final Path packageRoot;
    private final long maxPackageBytes;
    private final String signaturePublicKeyPem;

    @Autowired
    public PackageFileStorageService(
        @Value("${admin.upload.package-dir:${user.dir}/storage/packages}") String packageDir,
        @Value("${admin.upload.max-package-size:500MB}") String maxPackageSize,
        @Value("${admin.upload.signature-public-key-pem:}") String signaturePublicKeyPem
    ) {
        this.packageRoot = Path.of(packageDir).toAbsolutePath().normalize();
        this.maxPackageBytes = DataSize.parse(maxPackageSize).toBytes();
        this.signaturePublicKeyPem = signaturePublicKeyPem;
    }

    public PackageFileStorageService(String packageDir, String maxPackageSize) {
        this(packageDir, maxPackageSize, "");
    }

    public StoredPackage store(MultipartFile file, String packageFormat) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包文件不能为空");
        }
        if (file.getSize() > maxPackageBytes) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包不能超过 " + formatBytes(maxPackageBytes));
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        Path target = packageTarget(originalName, packageFormat);
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = file.getInputStream();
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                Files.copy(digestInput, target);
            }
            return new StoredPackage(
                originalName,
                file.getSize(),
                packageRoot.relativize(target).toString().replace('\\', '/'),
                HexFormat.of().formatHex(digest.digest())
            );
        } catch (Exception ex) {
            log.warn("Failed to store package file under {}", packageRoot, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "安装包保存失败");
        }
    }

    public VerifiedPackage storeAndVerify(
        MultipartFile file,
        String packageFormat,
        PackageVerificationRequest verificationRequest
    ) {
        StoredPackage storedPackage = store(file, packageFormat);
        try {
            return new VerifiedPackage(storedPackage, verifyStoredPackage(storedPackage, verificationRequest));
        } catch (RuntimeException ex) {
            deleteStoredPackageQuietly(storedPackage);
            throw ex;
        }
    }

    public VerifiedPackage storeCompletedFile(
        Path source,
        String originalFileName,
        String packageFormat,
        PackageVerificationRequest verificationRequest
    ) {
        Path normalizedSource = source.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedSource)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "待合并安装包不存在");
        }
        String originalName = sanitizeFileName(originalFileName);
        Path target = packageTarget(originalName, packageFormat);
        try {
            long fileSize = Files.size(normalizedSource);
            if (fileSize > maxPackageBytes) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包不能超过 " + formatBytes(maxPackageBytes));
            }
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(normalizedSource);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                Files.copy(digestInput, target);
            }
            StoredPackage storedPackage = new StoredPackage(
                originalName,
                fileSize,
                packageRoot.relativize(target).toString().replace('\\', '/'),
                HexFormat.of().formatHex(digest.digest())
            );
            try {
                return new VerifiedPackage(storedPackage, verifyStoredPackage(storedPackage, verificationRequest));
            } catch (RuntimeException ex) {
                deleteStoredPackageQuietly(storedPackage);
                throw ex;
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to store completed package file under {}", packageRoot, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "安装包保存失败");
        }
    }

    public PackageVerificationResult verifyStoredPackage(
        StoredPackage storedPackage,
        PackageVerificationRequest verificationRequest
    ) {
        PackageVerificationRequest request = verificationRequest == null
            ? PackageVerificationRequest.empty()
            : verificationRequest;
        String expectedSha256 = normalizeSha256(request.expectedSha256());
        if (StringUtils.hasText(expectedSha256) && !expectedSha256.equalsIgnoreCase(storedPackage.sha256())) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包 SHA256 与预期不一致");
        }

        String algorithm = normalizeSignatureAlgorithm(request.signatureAlgorithm());
        String signatureValue = normalizeText(request.signatureValue());
        if (!StringUtils.hasText(algorithm) && StringUtils.hasText(expectedSha256)) {
            algorithm = "sha256";
            signatureValue = expectedSha256;
        }
        if (!StringUtils.hasText(algorithm) && !StringUtils.hasText(signatureValue)) {
            return PackageVerificationResult.notVerified();
        }
        if (!StringUtils.hasText(algorithm)) {
            algorithm = "sha256";
        }

        if ("sha256".equals(algorithm)) {
            String digest = StringUtils.hasText(signatureValue) ? normalizeSha256(signatureValue) : expectedSha256;
            if (!StringUtils.hasText(digest)) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "SHA256 签名值不能为空");
            }
            if (!digest.equalsIgnoreCase(storedPackage.sha256())) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包签名校验失败");
            }
            return PackageVerificationResult.verified("sha256", digest);
        }

        if ("sha256-rsa".equals(algorithm)) {
            if (!StringUtils.hasText(signatureValue)) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "RSA 签名值不能为空");
            }
            verifyRsaSignature(storedPackage, signatureValue);
            return PackageVerificationResult.verified("sha256-rsa", signatureValue);
        }

        throw new BusinessException(ErrorCode.PARAM_FORMAT, "签名算法不支持");
    }

    public Path packageRoot() {
        return packageRoot;
    }

    public long maxPackageBytes() {
        return maxPackageBytes;
    }

    public void deleteStoredPackageQuietly(StoredPackage storedPackage) {
        if (storedPackage == null || !StringUtils.hasText(storedPackage.storagePath())) {
            return;
        }
        Path target = packageRoot.resolve(storedPackage.storagePath()).normalize();
        if (!target.startsWith(packageRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (Exception ex) {
            log.warn("Failed to cleanup stored package file {}", target, ex);
        }
    }

    public String sanitizeOriginalFileName(String originalName) {
        return sanitizeFileName(originalName);
    }

    public void validatePackageMetadata(String fileName, String packageFormat, long fileSize) {
        if (fileSize <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包大小必须大于0");
        }
        if (fileSize > maxPackageBytes) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包不能超过 " + formatBytes(maxPackageBytes));
        }
        validatePackageExtension(sanitizeFileName(fileName), packageFormat);
    }

    private void validatePackageExtension(String fileName, String packageFormat) {
        String normalizedFormat = normalizePackageFormat(packageFormat);
        Set<String> allowedExtensions = FORMAT_EXTENSIONS.get(normalizedFormat);
        if (allowedExtensions == null) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包格式不支持");
        }

        String extension = extensionOf(fileName);
        if (!allowedExtensions.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包后缀与包格式不一致，仅支持 .deb、.rpm、.appimage");
        }
    }

    private Path packageTarget(String originalName, String packageFormat) {
        validatePackageExtension(originalName, packageFormat);
        LocalDate today = LocalDate.now();
        Path relativePath = Path.of(
            Integer.toString(today.getYear()),
            String.format("%02d", today.getMonthValue()),
            UUID.randomUUID() + "-" + originalName
        );
        Path target = packageRoot.resolve(relativePath).normalize();
        if (!target.startsWith(packageRoot)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "文件名非法");
        }
        return target;
    }

    private String normalizePackageFormat(String packageFormat) {
        return packageFormat == null ? "" : packageFormat.trim().toLowerCase();
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private String formatBytes(long bytes) {
        long mb = 1024L * 1024L;
        if (bytes % mb == 0) {
            return (bytes / mb) + "MB";
        }
        return bytes + " bytes";
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeSha256(String value) {
        String text = normalizeText(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.toLowerCase();
        if (!normalized.matches("[0-9a-f]{64}")) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "SHA256 格式错误");
        }
        return normalized;
    }

    private String normalizeSignatureAlgorithm(String value) {
        String text = normalizeText(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.toLowerCase().replace("_", "-");
    }

    private void verifyRsaSignature(StoredPackage storedPackage, String signatureValue) {
        if (!StringUtils.hasText(signaturePublicKeyPem)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "未配置签名验签公钥");
        }
        Path packagePath = packageRoot.resolve(storedPackage.storagePath()).normalize();
        if (!packagePath.startsWith(packageRoot) || !Files.isRegularFile(packagePath)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "安装包文件不存在");
        }
        try {
            PublicKey publicKey = parseRsaPublicKey(signaturePublicKeyPem);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            try (InputStream input = Files.newInputStream(packagePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    signature.update(buffer, 0, read);
                }
            }
            if (!signature.verify(Base64.getDecoder().decode(signatureValue))) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包签名校验失败");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包签名校验失败");
        }
    }

    private PublicKey parseRsaPublicKey(String pem) throws Exception {
        String content = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String sanitizeFileName(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return "package.bin";
        }
        String sanitized = Path.of(originalName).getFileName().toString()
            .replaceAll("[^A-Za-z0-9._-]", "_");
        return StringUtils.hasText(sanitized) ? sanitized : "package.bin";
    }

    public record StoredPackage(String fileName, Long fileSize, String storagePath, String sha256) {
    }

    public record VerifiedPackage(StoredPackage storedPackage, PackageVerificationResult verificationResult) {
    }

    public record PackageVerificationRequest(String expectedSha256, String signatureAlgorithm, String signatureValue) {
        public static PackageVerificationRequest empty() {
            return new PackageVerificationRequest(null, null, null);
        }
    }

    public record PackageVerificationResult(
        String signatureAlgorithm,
        String signatureValue,
        Integer signatureStatus,
        LocalDateTime signatureVerifiedAt
    ) {
        public static PackageVerificationResult notVerified() {
            return new PackageVerificationResult(null, null, 0, null);
        }

        public static PackageVerificationResult verified(String signatureAlgorithm, String signatureValue) {
            return new PackageVerificationResult(signatureAlgorithm, signatureValue, 1, LocalDateTime.now());
        }
    }
}
