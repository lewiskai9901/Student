package com.school.management.service;

import com.school.management.dto.PermissionCreateRequest;
import com.school.management.dto.PermissionUpdateRequest;
import com.school.management.entity.Permission;

import java.util.List;

/**
 * 权限管理服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface PermissionService {

    /**
     * 创建权限
     *
     * @param request 创建请求
     * @return 权限ID
     */
    Long createPermission(PermissionCreateRequest request);

    /**
     * 更新权限
     *
     * @param request 更新请求
     */
    void updatePermission(PermissionUpdateRequest request);

    /**
     * 删除权限
     *
     * @param id 权限ID
     */
    void deletePermission(Long id);

    /**
     * 批量删除权限
     *
     * @param ids 权限ID列表
     */
    void deletePermissions(List<Long> ids);

    /**
     * 根据ID获取权限
     *
     * @param id 权限ID
     * @return 权限信息
     */
    Permission getPermissionById(Long id);

    /**
     * 根据权限编码获取权限
     *
     * @param permissionCode 权限编码
     * @return 权限信息
     */
    Permission getPermissionByCode(String permissionCode);

    /**
     * 获取权限树形结构
     *
     * @return 权限树
     */
    List<Permission> getPermissionTree();

    /**
     * 获取所有权限列表
     *
     * @return 权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 根据用户ID获取权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByUserId(Long userId);

    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleId(Long roleId);

    /**
     * 检查权限编码是否存在
     *
     * @param permissionCode 权限编码
     * @param excludeId 排除的权限ID
     * @return 是否存在
     */
    boolean existsPermissionCode(String permissionCode, Long excludeId);

    /**
     * 根据父权限ID获取子权限列表
     *
     * @param parentId 父权限ID
     * @return 子权限列表
     */
    List<Permission> getPermissionsByParentId(Long parentId);

    /**
     * 构建权限树形结构
     *
     * @param permissions 权限列表
     * @param parentId 父权限ID
     * @return 权限树
     */
    List<Permission> buildPermissionTree(List<Permission> permissions, Long parentId);

    /**
     * 获取用户的权限编码列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    List<String> getUserPermissionCodes(Long userId);

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否拥有权限
     */
    boolean hasPermission(Long userId, String permissionCode);
}