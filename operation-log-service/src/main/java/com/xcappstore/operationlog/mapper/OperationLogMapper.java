package com.xcappstore.operationlog.mapper;

import com.xcappstore.operationlog.dto.OperationLogQueryRequest;
import com.xcappstore.operationlog.dto.StatItem;
import com.xcappstore.operationlog.entity.OperationLogEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OperationLogMapper {
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
