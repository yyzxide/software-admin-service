package com.xcappstore.admin.software.service;

import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;

public interface SoftwareService {
    SoftwareResponse upload(SoftwareUploadRequest request, Long adminUserId);

    PageResponse<SoftwareResponse> page(SoftwareQueryRequest request);

    SoftwareResponse detail(Long id);

    SoftwareResponse publish(Long id, Long adminUserId);

    SoftwareResponse unpublish(Long id, Long adminUserId);
}
