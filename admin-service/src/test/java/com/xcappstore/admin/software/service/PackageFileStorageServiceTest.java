package com.xcappstore.admin.software.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationRequest;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class PackageFileStorageServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void storesPackageAndCalculatesSha256() throws Exception {
        PackageFileStorageService service = new PackageFileStorageService(tempDir.toString(), "1MB");
        MockMultipartFile file = new MockMultipartFile(
            "package_file",
            "editor.deb",
            "application/octet-stream",
            "hello".getBytes()
        );

        PackageFileStorageService.StoredPackage storedPackage = service.store(file, "deb");

        assertEquals("editor.deb", storedPackage.fileName());
        assertEquals(5L, storedPackage.fileSize());
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", storedPackage.sha256());
        assertEquals(true, Files.exists(tempDir.resolve(storedPackage.storagePath())));
    }

    @Test
    void verifiesExpectedSha256WhenStoringPackage() {
        PackageFileStorageService service = new PackageFileStorageService(tempDir.toString(), "1MB");
        MockMultipartFile file = new MockMultipartFile(
            "package_file",
            "editor.deb",
            "application/octet-stream",
            "hello".getBytes(StandardCharsets.UTF_8)
        );

        VerifiedPackage verifiedPackage = service.storeAndVerify(
            file,
            "deb",
            new PackageVerificationRequest(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                null,
                null
            )
        );

        assertEquals("editor.deb", verifiedPackage.storedPackage().fileName());
        assertEquals(1, verifiedPackage.verificationResult().signatureStatus());
        assertEquals("sha256", verifiedPackage.verificationResult().signatureAlgorithm());
    }

    @Test
    void rejectsWrongExpectedSha256() {
        PackageFileStorageService service = new PackageFileStorageService(tempDir.toString(), "1MB");
        MockMultipartFile file = new MockMultipartFile(
            "package_file",
            "editor.deb",
            "application/octet-stream",
            "hello".getBytes(StandardCharsets.UTF_8)
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.storeAndVerify(
            file,
            "deb",
            new PackageVerificationRequest(
                "0000000000000000000000000000000000000000000000000000000000000000",
                null,
                null
            )
        ));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("安装包 SHA256 与预期不一致", ex.getMessage());
    }

    @Test
    void storesCompletedChunkFile() throws Exception {
        PackageFileStorageService service = new PackageFileStorageService(tempDir.toString(), "1MB");
        Path mergedFile = tempDir.resolve("merged.deb");
        Files.writeString(mergedFile, "chunked-package", StandardCharsets.UTF_8);

        VerifiedPackage verifiedPackage = service.storeCompletedFile(
            mergedFile,
            "editor.deb",
            "deb",
            PackageVerificationRequest.empty()
        );

        assertEquals("editor.deb", verifiedPackage.storedPackage().fileName());
        assertEquals(15L, verifiedPackage.storedPackage().fileSize());
        assertEquals(0, verifiedPackage.verificationResult().signatureStatus());
        assertEquals(true, Files.exists(tempDir.resolve(verifiedPackage.storedPackage().storagePath())));
    }

    @Test
    void rejectsMismatchedExtensionAndFormat() {
        PackageFileStorageService service = new PackageFileStorageService(tempDir.toString(), "1MB");
        MockMultipartFile file = new MockMultipartFile(
            "package_file",
            "editor.zip",
            "application/octet-stream",
            "hello".getBytes()
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.store(file, "deb"));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("安装包后缀与包格式不一致，仅支持 .deb、.rpm、.appimage", ex.getMessage());
    }

    @Test
    void rejectsOversizedPackage() {
        PackageFileStorageService service = new PackageFileStorageService(tempDir.toString(), "4B");
        MockMultipartFile file = new MockMultipartFile(
            "package_file",
            "editor.deb",
            "application/octet-stream",
            "hello".getBytes()
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> service.store(file, "deb"));

        assertEquals(ErrorCode.PARAM_FORMAT, ex.getCode());
        assertEquals("安装包不能超过 4 bytes", ex.getMessage());
    }
}
