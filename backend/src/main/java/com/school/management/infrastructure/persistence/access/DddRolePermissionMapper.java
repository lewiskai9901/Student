package com.school.management.infrastructure.persistence.access;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for role-permission mappings (DDD infrastructure).
 */
@Mapper
public interface DddRolePermissionMapper extends BaseMapper<RolePermissionPO> {

    @Select("SELECT permission_id FROM role_permissions WHERE role_id = #{roleId}")
    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT role_id FROM role_permissions WHERE permission_id = #{permissionId}")
    List<Long> findRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    @Delete("DELETE FROM role_permissions WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM role_permissions WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    void deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    @Select("SELECT COUNT(*) > 0 FROM role_permissions WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    boolean existsByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
