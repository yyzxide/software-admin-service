package com.xcappstore.admin.software.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.dto.PackageUploadChunkRequest;
import com.xcappstore.admin.software.dto.PackageUploadCreateRequest;
import com.xcappstore.admin.software.dto.PackageUploadSessionResponse;
import com.xcappstore.admin.software.entity.PackageUploadSessionEntity;
import com.xcappstore.admin.software.mapper.PackageUploadSessionMapper;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class PackageUploadSessionServiceTest {
    private static final String HELLO_WORLD_SHA256 =
        "936a185caaa266bb9cbe981e9e05cb78cd732b0b3280eb944412bb6f8f8f07af";

    @TempDir
    private Path tempDir;

    private FakePackageUploadSessionMapper mapper;
    private PackageUploadSessionService service;

    @BeforeEach
    void setUp() {
        mapper = new FakePackageUploadSessionMapper();
        PackageFileStorageService storageService = new PackageFileStorageService(
            tempDir.resolve("packages").toString(),
            "1MB"
        );
        service = new PackageUploadSessionService(mapper, storageService, new ObjectMapper());
    }

    @Test
    void resumesChunkUploadAndCompletesSession() {
        PackageUploadSessionResponse created = service.create(createRequest(), 99L);

        assertEquals(2, created.getTotalChunks());
        assertEquals("上传中", created.getStatusText());

        PackageUploadSessionResponse progress = service.uploadChunk(
            created.getUploadId(),
            chunkRequest(1, "world"),
            99L
        );
        assertEquals(1, progress.getUploadedChunkCount());
        assertEquals(List.of(1), progress.getUploadedChunks());

        PackageUploadSessionResponse status = service.status(created.getUploadId(), 99L);
        assertEquals(List.of(1), status.getUploadedChunks());

        service.uploadChunk(created.getUploadId(), chunkRequest(0, "hello"), 99L);
        PackageUploadSessionResponse completed = service.complete(created.getUploadId(), 99L);

        assertEquals("已完成", completed.getStatusText());
        assertEquals(HELLO_WORLD_SHA256, completed.getActualSha256());
        assertEquals(1, completed.getSignatureStatus());
        assertEquals("sha256", completed.getSignatureAlgorithm());
        assertNotNull(completed.getCompletedAt());
        assertTrue(Files.exists(tempDir.resolve("packages").resolve(completed.getStoragePath())));

        VerifiedPackage consumed = service.consumeCompletedSession(created.getUploadId(), "deb", 99L);

        assertEquals("editor.deb", consumed.storedPackage().fileName());
        assertEquals(HELLO_WORLD_SHA256, consumed.storedPackage().sha256());
        assertEquals(2, mapper.sessions.get(created.getUploadId()).getStatus());

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.consumeCompletedSession(created.getUploadId(), "deb", 99L)
        );
        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
    }

    @Test
    void rejectsCompletingWhenChunksAreMissing() {
        PackageUploadSessionResponse created = service.create(createRequest(), 99L);
        service.uploadChunk(created.getUploadId(), chunkRequest(0, "hello"), 99L);

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.complete(created.getUploadId(), 99L)
        );

        assertEquals(ErrorCode.INVALID_STATUS_FLOW, ex.getCode());
        assertEquals("分片尚未全部上传", ex.getMessage());
    }

    @Test
    void rejectsAccessFromDifferentAdminUser() {
        PackageUploadSessionResponse created = service.create(createRequest(), 99L);

        BusinessException ex = assertThrows(
            BusinessException.class,
            () -> service.status(created.getUploadId(), 100L)
        );

        assertEquals(ErrorCode.PERMISSION_DENIED, ex.getCode());
    }

    @Test
    void cleansExpiredUploadingSessionAndDeletesChunks() {
        PackageUploadSessionResponse created = service.create(createRequest(), 99L);
        service.uploadChunk(created.getUploadId(), chunkRequest(0, "hello"), 99L);
        mapper.sessions.get(created.getUploadId()).setUpdatedAt(LocalDateTime.now().minusDays(2));

        int cleaned = service.cleanupExpiredUploadingSessions(LocalDateTime.now().minusDays(1), 100);

        assertEquals(1, cleaned);
        assertEquals(3, mapper.sessions.get(created.getUploadId()).getStatus());
        assertEquals("上传会话已过期，临时分片已清理", mapper.sessions.get(created.getUploadId()).getErrorMessage());
        assertEquals(false, Files.exists(tempDir.resolve("packages").resolve(".chunks").resolve(created.getUploadId())));
    }

    private PackageUploadCreateRequest createRequest() {
        PackageUploadCreateRequest request = new PackageUploadCreateRequest();
        request.setFileName("editor.deb");
        request.setPackageFormat("deb");
        request.setFileSize(10L);
        request.setChunkSize(5L);
        request.setExpectedSha256(HELLO_WORLD_SHA256);
        return request;
    }

    private PackageUploadChunkRequest chunkRequest(int chunkIndex, String content) {
        PackageUploadChunkRequest request = new PackageUploadChunkRequest();
        request.setChunkIndex(chunkIndex);
        request.setChunkFile(new MockMultipartFile(
            "chunk_file",
            chunkIndex + ".part",
            "application/octet-stream",
            content.getBytes(StandardCharsets.UTF_8)
        ));
        return request;
    }

    private static final class FakePackageUploadSessionMapper implements PackageUploadSessionMapper {
        private final Map<String, PackageUploadSessionEntity> sessions = new LinkedHashMap<>();

        @Override
        public int insert(PackageUploadSessionEntity entity) {
            sessions.put(entity.getUploadId(), entity);
            return 1;
        }

        @Override
        public PackageUploadSessionEntity selectByUploadId(String uploadId) {
            return sessions.get(uploadId);
        }

        @Override
        public List<PackageUploadSessionEntity> selectExpiredUploading(LocalDateTime cutoff, int limit) {
            return sessions.values().stream()
                .filter(session -> Integer.valueOf(0).equals(session.getStatus()))
                .filter(session -> session.getUpdatedAt().isBefore(cutoff))
                .limit(limit)
                .toList();
        }

        @Override
        public int updateProgress(String uploadId, String uploadedChunks, Integer uploadedChunkCount) {
            PackageUploadSessionEntity session = sessions.get(uploadId);
            session.setUploadedChunks(uploadedChunks);
            session.setUploadedChunkCount(uploadedChunkCount);
            session.setUpdatedAt(LocalDateTime.now());
            return 1;
        }

        @Override
        public int complete(
            String uploadId,
            String actualSha256,
            String storagePath,
            String signatureAlgorithm,
            String signatureValue,
            Integer signatureStatus,
            LocalDateTime signatureVerifiedAt,
            LocalDateTime completedAt
        ) {
            PackageUploadSessionEntity session = sessions.get(uploadId);
            session.setActualSha256(actualSha256);
            session.setStoragePath(storagePath);
            session.setSignatureAlgorithm(signatureAlgorithm);
            session.setSignatureValue(signatureValue);
            session.setSignatureStatus(signatureStatus);
            session.setSignatureVerifiedAt(signatureVerifiedAt);
            session.setStatus(1);
            session.setUpdatedAt(completedAt);
            session.setCompletedAt(completedAt);
            return 1;
        }

        @Override
        public int markConsumed(String uploadId) {
            PackageUploadSessionEntity session = sessions.get(uploadId);
            if (session == null || !Integer.valueOf(1).equals(session.getStatus())) {
                return 0;
            }
            session.setStatus(2);
            session.setUpdatedAt(LocalDateTime.now());
            return 1;
        }

        @Override
        public int markFailed(String uploadId, String errorMessage) {
            PackageUploadSessionEntity session = sessions.get(uploadId);
            if (session == null || !Integer.valueOf(0).equals(session.getStatus())) {
                return 0;
            }
            session.setStatus(3);
            session.setErrorMessage(errorMessage);
            session.setUpdatedAt(LocalDateTime.now());
            return 1;
        }
    }
}
