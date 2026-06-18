package com.xcappstore.admin.review.mapper;

import com.xcappstore.admin.review.dto.ReviewTaskQueryRequest;
import com.xcappstore.admin.review.entity.ReviewHistoryEntity;
import com.xcappstore.admin.review.entity.ReviewTaskEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewMapper {
    int insertTask(ReviewTaskEntity task);

    int insertHistory(ReviewHistoryEntity history);

    long countActiveTask(@Param("appId") Long appId, @Param("versionId") Long versionId);

    long count(@Param("query") ReviewTaskQueryRequest query);

    List<ReviewTaskEntity> selectPage(
        @Param("query") ReviewTaskQueryRequest query,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    ReviewTaskEntity selectById(@Param("id") Long id);

    List<ReviewHistoryEntity> selectHistories(@Param("taskId") Long taskId);

    int assign(
        @Param("id") Long id,
        @Param("reviewerId") Long reviewerId,
        @Param("status") Integer status,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    int finish(
        @Param("id") Long id,
        @Param("status") Integer status,
        @Param("reviewComment") String reviewComment,
        @Param("reviewerId") Long reviewerId,
        @Param("reviewedAt") LocalDateTime reviewedAt,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}
