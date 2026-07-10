package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.model.PackageScanStatus;
import com.xcappstore.admin.software.model.PackageStatus;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationRequest;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationResult;
import com.xcappstore.admin.software.service.PackageFileStorageService.StoredPackage;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PackagePreparationService {
    private static final Set<String> ALLOWED_OS_TYPES = Set.of("uos_v20", "uos_v23", "kylin_v10", "kylin_v11");
    private static final Set<String> ALLOWED_ARCHS = Set.of("x86_64", "aarch64", "loongarch64");
    private static final Set<String> ALLOWED_PACKAGE_FORMATS = Set.of("deb", "rpm", "appimage");

    private final PackageFileStorageService packageFileStorageService;
    private final PackageUploadSessionService packageUploadSessionService;

    public PackagePreparationService(
        PackageFileStorageService packageFileStorageService,
        PackageUploadSessionService packageUploadSessionService
    ) {
        this.packageFileStorageService = packageFileStorageService;
        this.packageUploadSessionService = packageUploadSessionService;
    }

    public PackageSpec requireSpec(String osType, String arch, String packageFormat) {
        return new PackageSpec(
            requireAllowed(normalizeText(osType), ALLOWED_OS_TYPES, "系统类型不支持"),
            requireAllowed(normalizeText(arch), ALLOWED_ARCHS, "CPU架构不支持"),
            requireAllowed(normalizeText(packageFormat), ALLOWED_PACKAGE_FORMATS, "安装包格式不支持")
        );
    }

    public VerifiedPackage resolvePackage(
        MultipartFile packageFile,
        String uploadSessionId,
        String packageFormat,
        String expectedSha256,
        String signatureAlgorithm,
        String signatureValue,
        Long operatorId
    ) {
        boolean hasUploadSession = StringUtils.hasText(uploadSessionId);
        boolean hasDirectFile = packageFile != null && !packageFile.isEmpty();
        if (hasUploadSession && hasDirectFile) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包文件和上传会话不能同时提交");
        }
        if (hasUploadSession) {
            return packageUploadSessionService.consumeCompletedSession(uploadSessionId, packageFormat, operatorId);
        }
        return packageFileStorageService.storeAndVerify(
            packageFile,
            packageFormat,
            new PackageVerificationRequest(expectedSha256, signatureAlgorithm, signatureValue)
        );
    }

    public AppPackageEntity buildAvailablePackage(
        Long appId,
        Long versionId,
        PackageSpec spec,
        VerifiedPackage verifiedPackage,
        Long operatorId,
        LocalDateTime now
    ) {
        StoredPackage storedPackage = verifiedPackage.storedPackage();
        AppPackageEntity packageInfo = new AppPackageEntity();
        packageInfo.setAppId(appId);
        packageInfo.setVersionId(versionId);
        packageInfo.setOsType(spec.osType());
        packageInfo.setArch(spec.arch());
        packageInfo.setPackageFormat(spec.packageFormat());
        packageInfo.setFileName(storedPackage.fileName());
        packageInfo.setFileSize(storedPackage.fileSize());
        packageInfo.setStoragePath(storedPackage.storagePath());
        packageInfo.setSha256(storedPackage.sha256());
        applyPackageVerification(packageInfo, verifiedPackage.verificationResult());
        packageInfo.setStatus(PackageStatus.AVAILABLE.code());
        packageInfo.setDownloadCount(0L);
        packageInfo.setScanStatus(PackageScanStatus.UNSCANNED.code());
        packageInfo.setCreatedAt(now);
        packageInfo.setUpdatedAt(now);
        packageInfo.setCreatedBy(operatorId);
        packageInfo.setUpdatedBy(operatorId);
        return packageInfo;
    }

    public void deleteStoredPackageQuietly(VerifiedPackage verifiedPackage) {
        if (verifiedPackage != null) {
            packageFileStorageService.deleteStoredPackageQuietly(verifiedPackage.storedPackage());
        }
    }

    private String requireAllowed(String value, Set<String> allowedValues, String message) {
        if (!StringUtils.hasText(value) || !allowedValues.contains(value)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, message);
        }
        return value;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private void applyPackageVerification(AppPackageEntity packageInfo, PackageVerificationResult verificationResult) {
        PackageVerificationResult result = verificationResult == null
            ? PackageVerificationResult.notVerified()
            : verificationResult;
        packageInfo.setSignatureAlgorithm(result.signatureAlgorithm());
        packageInfo.setSignatureValue(result.signatureValue());
        packageInfo.setSignatureStatus(result.signatureStatus());
        packageInfo.setSignatureVerifiedAt(result.signatureVerifiedAt());
    }

    public record PackageSpec(String osType, String arch, String packageFormat) {
    }
}
