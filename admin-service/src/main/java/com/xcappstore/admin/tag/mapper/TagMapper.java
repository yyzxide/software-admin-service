package com.xcappstore.admin.tag.mapper;

import com.xcappstore.admin.tag.entity.TagEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper {
    int insert(TagEntity tag);

    TagEntity selectById(@Param("id") Long id);

    List<TagEntity> selectList(@Param("isHot") Integer isHot, @Param("keyword") String keyword);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    long countApps(@Param("tagId") Long tagId);

    int update(TagEntity tag);

    int deleteById(@Param("id") Long id);
}
