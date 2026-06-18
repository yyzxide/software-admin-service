package com.xcappstore.admin.operationlog.mapper;

import com.xcappstore.admin.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.admin.operationlog.dto.StatItem;
import com.xcappstore.admin.operationlog.entity.OperationLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationLogMapper {
    int insert(OperationLogEntity entity);

    long count(@Param("query") OperationLogQueryRequest query);

    List<OperationLogEntity> selectPage(
        @Param("query") OperationLogQueryRequest query,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    OperationLogEntity selectById(@Param("id") Long id);

    List<String> selectActions();

    List<String> selectResourceTypes();

    List<StatItem> countByAction(@Param("query") OperationLogQueryRequest query);

    List<StatItem> countByResourceType(@Param("query") OperationLogQueryRequest query);

    List<StatItem> countByDate(@Param("query") OperationLogQueryRequest query);
}
