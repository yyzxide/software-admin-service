package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.PackageAppendRequest;
import com.xcappstore.admin.software.dto.PackageScanRequest;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareUpdateRequest;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.dto.VersionCreateRequest;
import java.util.List;

public interface SoftwareService {
    SoftwareResponse upload(SoftwareUploadRequest request, Long adminUserId);

    PageResponse<SoftwareResponse> page(SoftwareQueryRequest request);

    SoftwareResponse detail(Long id);

    SoftwareResponse update(Long id, SoftwareUpdateRequest request, Long adminUserId);

    SoftwareResponse publish(Long id, Long adminUserId);

    SoftwareResponse unpublish(Long id, Long adminUserId);

    AppVersionResponse addVersion(Long appId, VersionCreateRequest request, Long adminUserId);

    AppPackageResponse addPackage(Long appId, Long versionId, PackageAppendRequest request, Long adminUserId);

    AppPackageResponse scanPackage(Long appId, Long packageId, PackageScanRequest request, Long adminUserId);

    List<AppVersionResponse> versions(Long appId);

    List<AppPackageResponse> packages(Long appId);
}
