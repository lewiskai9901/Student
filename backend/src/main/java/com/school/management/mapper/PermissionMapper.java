package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据用户ID获取权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 获取所有权限树形结构
     *
     * @return 权限树
     */
    List<Permission> selectPermissionTree();

    /**
     * 根据权限编码获取权限
     *
     * @param permissionCode 权限编码
     * @return 权限
     */
    Permission selectByPermissionCode(@Param("permissionCode") String permissionCode);

    /**
     * 检查权限编码是否存在
     *
     * @param permissionCode 权限编码
     * @param excludeId 排除的权限ID
     * @return 数量
     */
    Integer countByPermissionCode(@Param("permissionCode") String permissionCode, @Param("excludeId") Long excludeId);

    /**
     * 获取子权限列表
     *
     * @param parentId 父权限ID
     * @return 子权限列表
     */
    List<Permission> selectByParentId(@Param("parentId") Long parentId);
}