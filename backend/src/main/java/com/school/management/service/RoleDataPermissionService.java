package com.school.management.service;

import com.school.management.dto.RoleDataPermissionDTO;

import java.util.List;

/**
 * 角色数据权限服务接口
 */
public interface RoleDataPermissionService {

    /**
     * 获取角色的数据权限配置
     * @param roleId 角色ID
     * @return 数据权限配置列表
     */
    List<RoleDataPermissionDTO> getRoleDataPermissions(Long roleId);

    /**
     * 保存角色的数据权限配置
     * @param roleId 角色ID
     * @param permissions 数据权限配置列表
     */
    void saveRoleDataPermissions(Long roleId, List<RoleDataPermissionDTO> permissions);

    /**
     * 获取所有模块列表
     * @return 模块列表
     */
    List<RoleDataPermissionDTO> getAllModules();
}
