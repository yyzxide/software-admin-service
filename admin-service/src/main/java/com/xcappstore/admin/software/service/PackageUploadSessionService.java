package com.xcappstore.admin.software.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.common.TransactionActions;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.dto.PackageUploadChunkRequest;
import com.xcappstore.admin.software.dto.PackageUploadCreateRequest;
import com.xcappstore.admin.software.dto.PackageUploadSessionResponse;
import com.xcappstore.admin.software.entity.PackageUploadSessionEntity;
import com.xcappstore.admin.software.mapper.PackageUploadSessionMapper;
import com.xcappstore.admin.software.model.SignatureStatus;
import com.xcappstore.admin.software.model.UploadSessionStatus;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationRequest;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationResult;
import com.xcappstore.admin.software.service.PackageFileStorageService.StoredPackage;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PackageUploadSessionService {
    private final PackageUploadSessionMapper mapper;
    private final PackageFileStorageService storageService;
    private final ObjectMapper objectMapper;

    public PackageUploadSessionService(
        PackageUploadSessionMapper mapper,
        PackageFileStorageService storageService,
        ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PackageUploadSessionResponse create(PackageUploadCreateRequest request, Long adminUserId) {
        String fileName = storageService.sanitizeOriginalFileName(request.getFileName());
        String packageFormat = normalizePackageFormat(request.getPackageFormat());
        if (request.getFileSize() == null || request.getFileSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "安装包大小必须大于0");
        }
        if (request.getChunkSize() == null || request.getChunkSize() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片大小必须大于0");
        }
        long fileSize = request.getFileSize();
        long chunkSize = request.getChunkSize();
        storageService.validatePackageMetadata(fileName, packageFormat, fileSize);
        if (chunkSize > storageService.maxPackageBytes()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片大小不能超过安装包大小上限");
        }
        int totalChunks = safeTotalChunks(fileSize, chunkSize);
        String uploadId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        PackageUploadSessionEntity entity = new PackageUploadSessionEntity();
        entity.setUploadId(uploadId);
        entity.setFileName(fileName);
        entity.setPackageFormat(packageFormat);
        entity.setFileSize(fileSize);
        entity.setChunkSize(chunkSize);
        entity.setTotalChunks(totalChunks);
        entity.setUploadedChunkCount(0);
        entity.setUploadedChunks("[]");
        entity.setExpectedSha256(normalizeText(request.getExpectedSha256()));
        entity.setSignatureAlgorithm(normalizeText(request.getSignatureAlgorithm()));
        entity.setSignatureValue(normalizeText(request.getSignatureValue()));
        entity.setSignatureStatus(SignatureStatus.NOT_VERIFIED.code());
        entity.setStatus(UploadSessionStatus.UPLOADING.code());
        entity.setCreatedBy(normalizeAdminUserId(adminUserId));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        createChunkDir(uploadId);
        return toResponse(entity);
    }

    @Transactional
    public PackageUploadSessionResponse uploadChunk(String uploadId, PackageUploadChunkRequest request, Long adminUserId) {
        PackageUploadSessionEntity session = requireUploadingSessionForUpdate(uploadId, adminUserId);
        if (request.getChunkIndex() == null) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片序号不能为空");
        }
        int chunkIndex = request.getChunkIndex();
        if (chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片序号超出范围");
        }
        if (request.getChunkFile() == null || request.getChunkFile().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片文件不能为空");
        }
        long chunkBytes = request.getChunkFile().getSize();
        validateChunkSize(session, chunkIndex, chunkBytes);

        Path chunkPath = chunkPath(session.getUploadId(), chunkIndex);
        storeChunkFile(request, chunkPath);

        List<Integer> uploadedChunks = uploadedChunksFromDisk(session);
        int updatedRows = mapper.updateProgress(session.getUploadId(), toJson(uploadedChunks), uploadedChunks.size());
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话状态已变化");
        }
        return toResponse(mapper.selectByUploadId(session.getUploadId()));
    }

    public PackageUploadSessionResponse status(String uploadId, Long adminUserId) {
        return toResponse(requireSession(uploadId, adminUserId));
    }

    public PackageUploadSessionResponse complete(String uploadId, Long adminUserId) {
        PackageUploadSessionEntity session = requireUploadingSession(uploadId, adminUserId);
        List<Integer> uploadedChunks = uploadedChunksFromDisk(session);
        if (uploadedChunks.size() != session.getTotalChunks()) {
            mapper.updateProgress(session.getUploadId(), toJson(uploadedChunks), uploadedChunks.size());
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "分片尚未全部上传");
        }
        if (mapper.markCompleting(session.getUploadId()) == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话状态已变化");
        }

        StoredPackage storedPackage = null;
        Path mergedPath = chunkDir(session.getUploadId()).resolve("merged.package").normalize();
        try {
            mergeChunks(session, mergedPath);
            long mergedSize = Files.size(mergedPath);
            if (mergedSize != session.getFileSize()) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "合并后文件大小与预期不一致");
            }
            VerifiedPackage verifiedPackage = storageService.storeCompletedFile(
                mergedPath,
                session.getFileName(),
                session.getPackageFormat(),
                verificationRequest(session)
            );
            storedPackage = verifiedPackage.storedPackage();
            PackageVerificationResult verificationResult = verifiedPackage.verificationResult();
            LocalDateTime completedAt = LocalDateTime.now();
            int updated = mapper.complete(
                session.getUploadId(),
                storedPackage.sha256(),
                storedPackage.storagePath(),
                verificationResult.signatureAlgorithm(),
                verificationResult.signatureValue(),
                verificationResult.signatureStatus(),
                verificationResult.signatureVerifiedAt(),
                completedAt
            );
            if (updated == 0) {
                storageService.deleteStoredPackageQuietly(storedPackage);
                throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话状态已变化");
            }
            deleteChunkDir(session.getUploadId());
            return toResponse(mapper.selectByUploadId(session.getUploadId()));
        } catch (BusinessException ex) {
            storageService.deleteStoredPackageQuietly(storedPackage);
            mapper.markFailed(session.getUploadId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            storageService.deleteStoredPackageQuietly(storedPackage);
            mapper.markFailed(session.getUploadId(), "分片合并失败");
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分片合并失败");
        }
    }

    @Transactional
    public VerifiedPackage consumeCompletedSession(String uploadId, String packageFormat, Long adminUserId) {
        if (!StringUtils.hasText(uploadId)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "上传会话ID不能为空");
        }
        PackageUploadSessionEntity session = requireSession(uploadId, adminUserId);
        UploadSessionStatus status = UploadSessionStatus.fromCode(session.getStatus());
        if (status == UploadSessionStatus.CONSUMED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话已被使用");
        }
        if (status != UploadSessionStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话尚未完成");
        }
        String normalizedFormat = normalizePackageFormat(packageFormat);
        if (!session.getPackageFormat().equals(normalizedFormat)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "上传会话包格式与业务请求不一致");
        }
        if (!StringUtils.hasText(session.getStoragePath()) || !StringUtils.hasText(session.getActualSha256())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话缺少已合并安装包");
        }
        if (mapper.markConsumed(uploadId) == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话状态已变化");
        }
        StoredPackage storedPackage = new StoredPackage(
            session.getFileName(),
            session.getFileSize(),
            session.getStoragePath(),
            session.getActualSha256()
        );
        PackageVerificationResult verificationResult = new PackageVerificationResult(
            session.getSignatureAlgorithm(),
            session.getSignatureValue(),
            session.getSignatureStatus(),
            session.getSignatureVerifiedAt()
        );
        return new VerifiedPackage(storedPackage, verificationResult);
    }

    @Transactional
    public int cleanupExpiredUploadingSessions(LocalDateTime cutoff, int limit) {
        if (cutoff == null) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "清理截止时间不能为空");
        }
        int normalizedLimit = Math.max(1, Math.min(limit, 500));
        List<PackageUploadSessionEntity> expiredSessions = mapper.selectExpiredUploading(cutoff, normalizedLimit);
        int cleaned = 0;
        for (PackageUploadSessionEntity session : expiredSessions) {
            int affected = mapper.markExpiredFailed(session.getUploadId(), cutoff, "上传会话已过期，临时分片已清理");
            if (affected > 0) {
                cleaned += affected;
                TransactionActions.afterCommit(() -> deleteChunkDir(session.getUploadId()));
            }
        }
        return cleaned;
    }

    protected PackageUploadSessionEntity requireSession(String uploadId, Long adminUserId) {
        if (!StringUtils.hasText(uploadId)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "上传会话ID不能为空");
        }
        PackageUploadSessionEntity session = mapper.selectByUploadId(uploadId);
        if (session == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "上传会话不存在");
        }
        Long operatorId = normalizeAdminUserId(adminUserId);
        if (session.getCreatedBy() != null && session.getCreatedBy() > 0 && !session.getCreatedBy().equals(operatorId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问该上传会话");
        }
        return session;
    }

    protected PackageUploadSessionResponse toResponse(PackageUploadSessionEntity entity) {
        PackageUploadSessionResponse response = new PackageUploadSessionResponse();
        response.setUploadId(entity.getUploadId());
        response.setFileName(entity.getFileName());
        response.setPackageFormat(entity.getPackageFormat());
        response.setFileSize(entity.getFileSize());
        response.setChunkSize(entity.getChunkSize());
        response.setTotalChunks(entity.getTotalChunks());
        response.setUploadedChunkCount(entity.getUploadedChunkCount());
        response.setUploadedChunks(parseChunks(entity.getUploadedChunks()));
        response.setExpectedSha256(entity.getExpectedSha256());
        response.setActualSha256(entity.getActualSha256());
        response.setStoragePath(entity.getStoragePath());
        response.setSignatureAlgorithm(entity.getSignatureAlgorithm());
        response.setSignatureStatus(entity.getSignatureStatus());
        response.setSignatureStatusText(SignatureStatus.fromCode(entity.getSignatureStatus()).text());
        response.setStatus(entity.getStatus());
        response.setStatusText(UploadSessionStatus.fromCode(entity.getStatus()).text());
        response.setErrorMessage(entity.getErrorMessage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }

    private PackageUploadSessionEntity requireUploadingSession(String uploadId, Long adminUserId) {
        PackageUploadSessionEntity session = requireSession(uploadId, adminUserId);
        if (!UploadSessionStatus.fromCode(session.getStatus()).isUploading()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话不在上传中状态");
        }
        return session;
    }

    private PackageUploadSessionEntity requireUploadingSessionForUpdate(String uploadId, Long adminUserId) {
        if (!StringUtils.hasText(uploadId)) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "上传会话ID不能为空");
        }
        PackageUploadSessionEntity session = mapper.selectByUploadIdForUpdate(uploadId);
        if (session == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "上传会话不存在");
        }
        Long operatorId = normalizeAdminUserId(adminUserId);
        if (session.getCreatedBy() != null && session.getCreatedBy() > 0 && !session.getCreatedBy().equals(operatorId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权访问该上传会话");
        }
        if (!UploadSessionStatus.fromCode(session.getStatus()).isUploading()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话不在上传中状态");
        }
        return session;
    }

    private int safeTotalChunks(long fileSize, long chunkSize) {
        long total = (fileSize + chunkSize - 1) / chunkSize;
        if (total > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片数量过多");
        }
        return (int) total;
    }

    private void validateChunkSize(PackageUploadSessionEntity session, int chunkIndex, long chunkBytes) {
        long expectedMax = session.getChunkSize();
        if (chunkBytes > expectedMax) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "分片大小超过会话分片大小");
        }
        boolean isLastChunk = chunkIndex == session.getTotalChunks() - 1;
        if (!isLastChunk && chunkBytes != expectedMax) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "非最后分片大小必须等于会话分片大小");
        }
        if (isLastChunk) {
            long expectedLastSize = session.getFileSize() - (long) chunkIndex * session.getChunkSize();
            if (chunkBytes != expectedLastSize) {
                throw new BusinessException(ErrorCode.PARAM_FORMAT, "最后分片大小与预期不一致");
            }
        }
    }

    private void storeChunkFile(PackageUploadChunkRequest request, Path chunkPath) {
        try {
            Files.createDirectories(chunkPath.getParent());
            Path tempPath = chunkPath.resolveSibling(chunkPath.getFileName() + "." + UUID.randomUUID() + ".tmp");
            try {
                try (InputStream input = request.getChunkFile().getInputStream()) {
                    Files.copy(input, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }
                try {
                    Files.move(tempPath, chunkPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tempPath, chunkPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tempPath);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分片保存失败");
        }
    }

    private void mergeChunks(PackageUploadSessionEntity session, Path mergedPath) throws Exception {
        Files.createDirectories(mergedPath.getParent());
        try (OutputStream output = Files.newOutputStream(mergedPath)) {
            byte[] buffer = new byte[8192];
            for (int index = 0; index < session.getTotalChunks(); index++) {
                Path chunkPath = chunkPath(session.getUploadId(), index);
                if (!Files.isRegularFile(chunkPath)) {
                    throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "缺少分片: " + index);
                }
                try (InputStream input = Files.newInputStream(chunkPath)) {
                    int read;
                    while ((read = input.read(buffer)) >= 0) {
                        output.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private List<Integer> uploadedChunksFromDisk(PackageUploadSessionEntity session) {
        List<Integer> uploadedChunks = new ArrayList<>();
        for (int index = 0; index < session.getTotalChunks(); index++) {
            if (Files.isRegularFile(chunkPath(session.getUploadId(), index))) {
                uploadedChunks.add(index);
            }
        }
        return uploadedChunks;
    }

    private Path chunkDir(String uploadId) {
        Path dir = storageService.packageRoot().resolve(".chunks").resolve(uploadId).normalize();
        if (!dir.startsWith(storageService.packageRoot())) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, "上传会话ID非法");
        }
        return dir;
    }

    private Path chunkPath(String uploadId, int index) {
        return chunkDir(uploadId).resolve(index + ".part").normalize();
    }

    private void createChunkDir(String uploadId) {
        try {
            Files.createDirectories(chunkDir(uploadId));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传会话目录创建失败");
        }
    }

    private void deleteChunkDir(String uploadId) {
        Path dir = chunkDir(uploadId);
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // best-effort cleanup for temporary chunks
                }
            });
        } catch (Exception ignored) {
            // best-effort cleanup for temporary chunks
        }
    }

    private String toJson(List<Integer> chunks) {
        try {
            return objectMapper.writeValueAsString(chunks);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "上传进度序列化失败");
        }
    }

    private List<Integer> parseChunks(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private PackageVerificationRequest verificationRequest(PackageUploadSessionEntity session) {
        return new PackageVerificationRequest(
            session.getExpectedSha256(),
            session.getSignatureAlgorithm(),
            session.getSignatureValue()
        );
    }

    private Long normalizeAdminUserId(Long adminUserId) {
        return adminUserId == null || adminUserId <= 0 ? 0L : adminUserId;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizePackageFormat(String value) {
        String text = normalizeText(value);
        return text == null ? null : text.toLowerCase();
    }
}
