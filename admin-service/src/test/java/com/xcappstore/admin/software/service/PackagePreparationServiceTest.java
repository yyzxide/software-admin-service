package com.xcappstore.admin.software.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationResult;
import com.xcappstore.admin.software.service.PackageFileStorageService.StoredPackage;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import com.xcappstore.admin.software.service.PackagePreparationService.PackageSpec;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PackagePreparationServiceTest {
    private final PackagePreparationService service = new PackagePreparationService(null, null);

    @Test
    void trimsAndValidatesPackageSpec() {
        PackageSpec spec = service.requireSpec(" uos_v20 ", " x86_64 ", " deb ");

        assertEquals("uos_v20", spec.osType());
        assertEquals("x86_64", spec.arch());
        assertEquals("deb", spec.packageFormat());
    }

    @Test
    void rejectsUnsupportedPackageSpec() {
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.requireSpec("uos_v20", "mips64", "deb")
        );

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("CPU架构不支持", ex.getMessage());
    }

    @Test
    void rejectsDirectFileAndUploadSessionTogether() {
        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.resolvePackage(
                new MockMultipartFile("package_file", "editor.deb", "application/octet-stream", "deb".getBytes()),
                "upload-session-1",
                "deb",
                null,
                null,
                null,
                99L
            )
        );

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("安装包文件和上传会话不能同时提交", ex.getMessage());
    }

    @Test
    void buildsAvailablePackageWithVerificationMetadata() {
        LocalDateTime now = LocalDateTime.now();
        AppPackageEntity packageInfo = service.buildAvailablePackage(
            1L,
            2L,
            new PackageSpec("uos_v20", "x86_64", "deb"),
            new VerifiedPackage(
                new StoredPackage("editor.deb", 12L, "2026/06/editor.deb", "sha256-value"),
                PackageVerificationResult.verified("sha256", "sha256-value")
            ),
            99L,
            now
        );

        assertEquals(1L, packageInfo.getAppId());
        assertEquals(2L, packageInfo.getVersionId());
        assertEquals("uos_v20", packageInfo.getOsType());
        assertEquals("x86_64", packageInfo.getArch());
        assertEquals("deb", packageInfo.getPackageFormat());
        assertEquals("editor.deb", packageInfo.getFileName());
        assertEquals(1, packageInfo.getStatus());
        assertEquals(0, packageInfo.getScanStatus());
        assertEquals(1, packageInfo.getSignatureStatus());
        assertEquals("sha256", packageInfo.getSignatureAlgorithm());
        assertEquals(99L, packageInfo.getCreatedBy());
        assertEquals(now, packageInfo.getUpdatedAt());
    }
}
