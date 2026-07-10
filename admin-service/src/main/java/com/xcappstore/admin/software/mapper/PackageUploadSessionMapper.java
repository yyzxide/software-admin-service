package com.xcappstore.admin.software.mapper;

import com.xcappstore.admin.software.entity.PackageUploadSessionEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PackageUploadSessionMapper {
    int insert(PackageUploadSessionEntity entity);

    PackageUploadSessionEntity selectByUploadId(@Param("uploadId") String uploadId);

    List<PackageUploadSessionEntity> selectExpiredUploading(
        @Param("cutoff") LocalDateTime cutoff,
        @Param("limit") int limit
    );

    int updateProgress(
        @Param("uploadId") String uploadId,
        @Param("uploadedChunks") String uploadedChunks,
        @Param("uploadedChunkCount") Integer uploadedChunkCount
    );

    int markCompleting(@Param("uploadId") String uploadId);

    int complete(
        @Param("uploadId") String uploadId,
        @Param("actualSha256") String actualSha256,
        @Param("storagePath") String storagePath,
        @Param("signatureAlgorithm") String signatureAlgorithm,
        @Param("signatureValue") String signatureValue,
        @Param("signatureStatus") Integer signatureStatus,
        @Param("signatureVerifiedAt") LocalDateTime signatureVerifiedAt,
        @Param("completedAt") LocalDateTime completedAt
    );

    int markConsumed(@Param("uploadId") String uploadId);

    int markFailed(@Param("uploadId") String uploadId, @Param("errorMessage") String errorMessage);
}
