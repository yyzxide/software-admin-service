package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PackageSecurityPolicyService {
    private static final int SIGNATURE_FAILED = 2;
    private static final int SCAN_SAFE = 1;

    private final SoftwareMapper softwareMapper;

    public PackageSecurityPolicyService(SoftwareMapper softwareMapper) {
        this.softwareMapper = softwareMapper;
    }

    public void assertAppPackagesPublishable(Long appId) {
        assertPackagesPublishable(softwareMapper.selectPackagesByAppId(appId));
    }

    public void assertVersionPackagesPublishable(Long appId, Long versionId) {
        List<AppPackageEntity> packages = softwareMapper.selectPackagesByAppId(appId)
            .stream()
            .filter(packageInfo -> versionId.equals(packageInfo.getVersionId()))
            .toList();
        assertPackagesPublishable(packages);
    }

    protected void assertPackagesPublishable(List<AppPackageEntity> packages) {
        for (AppPackageEntity packageInfo : packages) {
            if (Integer.valueOf(SIGNATURE_FAILED).equals(packageInfo.getSignatureStatus())) {
                throw new BusinessException(
                    ErrorCode.INVALID_STATUS_FLOW,
                    "安装包签名校验失败，不能上架: " + packageInfo.getFileName()
                );
            }
            if (!Integer.valueOf(SCAN_SAFE).equals(packageInfo.getScanStatus())) {
                throw new BusinessException(
                    ErrorCode.INVALID_STATUS_FLOW,
                    "安装包未通过安全扫描，不能上架: " + packageInfo.getFileName()
                );
            }
        }
    }
}
