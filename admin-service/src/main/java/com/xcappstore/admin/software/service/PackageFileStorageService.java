package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PackageFileStorageService {
    private static final Logger log = LoggerFactory.getLogger(PackageFileStorageService.class);

    private final Path packageRoot;

    public PackageFileStorageService(@Value("${admin.upload.package-dir:${user.dir}/storage/packages}") String packageDir) {
        this.packageRoot = Path.of(packageDir).toAbsolutePath().normalize();
    }

    public StoredPackage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包文件不能为空");
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
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
}
