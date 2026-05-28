package com.xcappstore.admin.software.mapper;

import com.xcappstore.admin.software.dto.SoftwareQueryRequest;
import com.xcappstore.admin.software.entity.AppPackageEntity;
import com.xcappstore.admin.software.entity.AppVersionEntity;
import com.xcappstore.admin.software.entity.SoftwareEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SoftwareMapper {
    long countByAppKey(@Param("appKey") String appKey);

    long countCategory(@Param("categoryId") Long categoryId);

    long countTag(@Param("tagId") Long tagId);

    int insertApp(SoftwareEntity app);

    int insertVersion(AppVersionEntity version);

    int insertPackage(AppPackageEntity packageInfo);

    int insertAppTag(@Param("appId") Long appId, @Param("tagId") Long tagId);

    long countList(@Param("query") SoftwareQueryRequest query);

    List<SoftwareEntity> selectList(
        @Param("query") SoftwareQueryRequest query,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    SoftwareEntity selectById(@Param("id") Long id);

    int updateStatus(
        @Param("id") Long id,
        @Param("status") Integer status,
        @Param("publishedAt") LocalDateTime publishedAt,
        @Param("updatedBy") Long updatedBy
    );

    int approveDraftVersions(
        @Param("appId") Long appId,
        @Param("reviewedAt") LocalDateTime reviewedAt,
        @Param("publishedAt") LocalDateTime publishedAt,
        @Param("updatedBy") Long updatedBy
    );
}
