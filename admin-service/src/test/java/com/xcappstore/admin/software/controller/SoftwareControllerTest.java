package com.xcappstore.admin.software.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xcappstore.admin.auth.AdminAuthInterceptor;
import com.xcappstore.admin.auth.AdminPrincipal;
import com.xcappstore.admin.common.PageResponse;
import com.xcappstore.admin.exception.GlobalExceptionHandler;
import com.xcappstore.admin.software.dto.AppPackageResponse;
import com.xcappstore.admin.software.dto.AppVersionResponse;
import com.xcappstore.admin.software.dto.PackageAppendRequest;
import com.xcappstore.admin.software.dto.PackageScanRequest;
import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.dto.SoftwareResponse;
import com.xcappstore.admin.software.dto.SoftwareUpdateRequest;
import com.xcappstore.admin.software.dto.SoftwareUploadRequest;
import com.xcappstore.admin.software.dto.VersionCreateRequest;
import com.xcappstore.admin.software.service.SoftwareService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SoftwareControllerTest {
    @Test
    void bindsMultipartUploadRequestWithCamelCaseFields() throws Exception {
        FakeSoftwareService softwareService = new FakeSoftwareService();
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new SoftwareController(softwareService))
            .setValidator(validator())
            .build();

        MockMultipartFile packageFile = new MockMultipartFile(
            "packageFile",
            "editor.deb",
            "application/octet-stream",
            "package".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/software/apps")
                .file(packageFile)
                .requestAttr(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR, new AdminPrincipal(99L, "tester", "admin", 0L))
                .param("appKey", "com.example.editor")
                .param("name", "文本编辑器")
                .param("categoryId", "1")
                .param("versionName", "1.0.0")
                .param("versionCode", "100")
                .param("osType", "uos_v20")
                .param("arch", "x86_64")
                .param("packageFormat", "deb")
                .param("summary", "国产系统文本编辑器")
                .param("description", "description")
                .param("publishNow", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(1));

        assertNotNull(softwareService.lastUpload);
        assertEquals("com.example.editor", softwareService.lastUpload.getAppKey());
        assertEquals(1L, softwareService.lastUpload.getCategoryId());
        assertEquals("editor.deb", softwareService.lastUpload.getPackageFile().getOriginalFilename());
        assertEquals(99L, softwareService.lastAdminUserId);
    }

    @Test
    void rejectsBlankScanResult() throws Exception {
        FakeSoftwareService softwareService = new FakeSoftwareService();
        MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new SoftwareController(softwareService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator())
            .build();

        mockMvc.perform(post("/api/v1/admin/software/apps/1/packages/10/scan")
                .requestAttr(AdminAuthInterceptor.ADMIN_PRINCIPAL_ATTR, new AdminPrincipal(99L, "tester", "admin", 0L))
                .contentType("application/json")
                .content("{\"result\":\" \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("安全状态结果不能为空"));
    }

    private LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return validator;
    }

    private static final class FakeSoftwareService implements SoftwareService {
        private SoftwareUploadRequest lastUpload;
        private Long lastAdminUserId;

        @Override
        public SoftwareResponse upload(SoftwareUploadRequest request, Long adminUserId) {
            this.lastUpload = request;
            this.lastAdminUserId = adminUserId;
            SoftwareResponse response = new SoftwareResponse();
            response.setId(1L);
            response.setAppKey(request.getAppKey());
            response.setName(request.getName());
            return response;
        }

        @Override
        public PageResponse<SoftwareResponse> page(SoftwareQueryRequest request) {
            return new PageResponse<>(0L, 1, 20, List.of());
        }

        @Override
        public SoftwareResponse detail(Long id) {
            return null;
        }

        @Override
        public SoftwareResponse update(Long id, SoftwareUpdateRequest request, Long adminUserId) {
            return null;
        }

        @Override
        public SoftwareResponse publish(Long id, Long adminUserId) {
            return null;
        }

        @Override
        public SoftwareResponse unpublish(Long id, Long adminUserId) {
            return null;
        }

        @Override
        public AppVersionResponse addVersion(Long appId, VersionCreateRequest request, Long adminUserId) {
            return null;
        }

        @Override
        public AppPackageResponse addPackage(Long appId, Long versionId, PackageAppendRequest request, Long adminUserId) {
            return null;
        }

        @Override
        public AppPackageResponse scanPackage(Long appId, Long packageId, PackageScanRequest request, Long adminUserId) {
            return null;
        }

        @Override
        public List<AppVersionResponse> versions(Long appId) {
            return List.of();
        }

        @Override
        public List<AppPackageResponse> packages(Long appId) {
            return List.of();
        }
    }
}
