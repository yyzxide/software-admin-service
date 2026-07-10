package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.mapper.SoftwareMapper;
import com.xcappstore.admin.software.model.PackageScanStatus;
import com.xcappstore.admin.software.model.SignatureStatus;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PackageSecurityPolicyService {
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
            if (SignatureStatus.FAILED == SignatureStatus.fromCode(packageInfo.getSignatureStatus())) {
                throw new BusinessException(
                    ErrorCode.INVALID_STATUS_FLOW,
                    "安装包签名校验失败，不能上架: " + packageInfo.getFileName()
                );
            }
            if (PackageScanStatus.SAFE != PackageScanStatus.fromCode(packageInfo.getScanStatus())) {
                throw new BusinessException(
                    ErrorCode.INVALID_STATUS_FLOW,
                    "安装包未通过安全状态校验，不能上架: " + packageInfo.getFileName()
                );
            }
        }
    }
}
