package com.xcappstore.admin.auth.rbac;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminRbacMapper {
    AdminUserEntity selectUserById(@Param("id") Long id);

    AdminUserEntity selectUserByUsername(@Param("username") String username);

    List<AdminUserEntity> selectUsers(@Param("keyword") String keyword, @Param("status") Integer status);

    long countUsersByUsername(@Param("username") String username, @Param("excludeId") Long excludeId);

    int insertUser(AdminUserEntity user);

    int updateUser(AdminUserEntity user);

    int updateUserStatus(@Param("id") Long id, @Param("status") Integer status);

    int updateUserPassword(@Param("id") Long id, @Param("passwordSha256") String passwordSha256);

    List<AdminRoleEntity> selectRoles(@Param("keyword") String keyword, @Param("status") Integer status);

    AdminRoleEntity selectRoleById(@Param("id") Long id);

    long countRolesByCode(@Param("roleCode") String roleCode, @Param("excludeId") Long excludeId);

    long countRolesByIds(@Param("roleIds") List<Long> roleIds);

    int insertRole(AdminRoleEntity role);

    int updateRole(AdminRoleEntity role);

    List<AdminPermissionEntity> selectPermissions(@Param("module") String module, @Param("status") Integer status);

    long countPermissionsByIds(@Param("permissionIds") List<Long> permissionIds);

    List<AdminRoleEntity> selectRolesByUserId(@Param("userId") Long userId);

    List<AdminPermissionEntity> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
