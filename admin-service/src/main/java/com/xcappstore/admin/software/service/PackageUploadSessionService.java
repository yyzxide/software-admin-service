package com.xcappstore.admin.software.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcappstore.admin.common.ErrorCode;
import com.xcappstore.admin.exception.BusinessException;
import com.xcappstore.admin.software.dto.PackageUploadChunkRequest;
import com.xcappstore.admin.software.dto.PackageUploadCreateRequest;
import com.xcappstore.admin.software.dto.PackageUploadSessionResponse;
import com.xcappstore.admin.software.entity.PackageUploadSessionEntity;
import com.xcappstore.admin.software.mapper.PackageUploadSessionMapper;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationRequest;
import com.xcappstore.admin.software.service.PackageFileStorageService.PackageVerificationResult;
import com.xcappstore.admin.software.service.PackageFileStorageService.StoredPackage;
import com.xcappstore.admin.software.service.PackageFileStorageService.VerifiedPackage;
import java.io.InputStream;
import java.io.OutputStream;
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
    private static final int STATUS_UPLOADING = 0;
    private static final int STATUS_COMPLETED = 1;
    private static final int STATUS_CONSUMED = 2;
    private static final int STATUS_FAILED = 3;

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
        entity.setSignatureStatus(0);
        entity.setStatus(STATUS_UPLOADING);
        entity.setCreatedBy(normalizeAdminUserId(adminUserId));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        createChunkDir(uploadId);
        return toResponse(entity);
    }

    @Transactional
    public PackageUploadSessionResponse uploadChunk(String uploadId, PackageUploadChunkRequest request, Long adminUserId) {
        PackageUploadSessionEntity session = requireUploadingSession(uploadId, adminUserId);
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
        try {
            Files.createDirectories(chunkPath.getParent());
            Files.copy(request.getChunkFile().getInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "分片保存失败");
        }

        List<Integer> uploadedChunks = uploadedChunksFromDisk(session);
        int updatedRows = mapper.updateProgress(session.getUploadId(), toJson(uploadedChunks), uploadedChunks.size());
        if (updatedRows == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话状态已变化");
        }
        PackageUploadSessionEntity updatedSession = mapper.selectByUploadId(session.getUploadId());
        return toResponse(updatedSession);
    }

    public PackageUploadSessionResponse status(String uploadId, Long adminUserId) {
        return toResponse(requireSession(uploadId, adminUserId));
    }

    @Transactional
    public PackageUploadSessionResponse complete(String uploadId, Long adminUserId) {
        PackageUploadSessionEntity session = requireUploadingSession(uploadId, adminUserId);
        List<Integer> uploadedChunks = uploadedChunksFromDisk(session);
        if (uploadedChunks.size() != session.getTotalChunks()) {
            mapper.updateProgress(session.getUploadId(), toJson(uploadedChunks), uploadedChunks.size());
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "分片尚未全部上传");
        }
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
            StoredPackage storedPackage = verifiedPackage.storedPackage();
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
                throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话状态已变化");
            }
            deleteChunkDir(session.getUploadId());
            return toResponse(mapper.selectByUploadId(session.getUploadId()));
        } catch (BusinessException ex) {
            mapper.markFailed(session.getUploadId(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
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
        if (STATUS_CONSUMED == safeStatus(session)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话已被使用");
        }
        if (STATUS_COMPLETED != safeStatus(session)) {
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
            deleteChunkDir(session.getUploadId());
            cleaned += mapper.markFailed(session.getUploadId(), "上传会话已过期，临时分片已清理");
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
        response.setSignatureStatusText(signatureStatusText(entity.getSignatureStatus()));
        response.setStatus(entity.getStatus());
        response.setStatusText(statusText(entity.getStatus()));
        response.setErrorMessage(entity.getErrorMessage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }

    private PackageUploadSessionEntity requireUploadingSession(String uploadId, Long adminUserId) {
        PackageUploadSessionEntity session = requireSession(uploadId, adminUserId);
        if (STATUS_UPLOADING != safeStatus(session)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_FLOW, "上传会话不在上传中状态");
        }
        return session;
    }

    private int safeStatus(PackageUploadSessionEntity session) {
        return session.getStatus() == null ? STATUS_FAILED : session.getStatus();
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

    private String statusText(Integer status) {
        return switch (status == null ? STATUS_FAILED : status) {
            case STATUS_UPLOADING -> "上传中";
            case STATUS_COMPLETED -> "已完成";
            case STATUS_CONSUMED -> "已使用";
            case STATUS_FAILED -> "失败";
            default -> "未知";
        };
    }

    private String signatureStatusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 0 -> "未校验";
            case 1 -> "通过";
            case 2 -> "失败";
            default -> "未知";
        };
    }
}
