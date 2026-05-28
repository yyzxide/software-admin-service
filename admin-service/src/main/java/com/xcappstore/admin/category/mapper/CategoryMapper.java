package com.xcappstore.admin.category.mapper;

import com.xcappstore.admin.category.entity.CategoryEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CategoryMapper {
    int insert(CategoryEntity category);

    CategoryEntity selectById(@Param("id") Long id);

    List<CategoryEntity> selectList(@Param("parentId") Long parentId, @Param("status") Integer status);

    List<CategoryEntity> selectAll(@Param("status") Integer status);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    long countChildren(@Param("parentId") Long parentId);

    long countApps(@Param("categoryId") Long categoryId);

    int update(CategoryEntity category);

    int softDelete(@Param("id") Long id);
}
