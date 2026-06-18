package com.xcappstore.admin.auth.rbac;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminPermissionMapper {
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
