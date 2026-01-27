package com.school.management.service;

import com.school.management.casbin.model.DataModule;
import com.school.management.casbin.model.DataScope;
import com.school.management.dto.RoleDataPermissionDTO;

import java.util.List;
import java.util.Map;

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

    // ==================== V2 API Methods ====================

    /**
     * V2: 获取角色的完整数据权限配置
     * @param roleId 角色ID
     * @return 包含自定义范围的完整配置
     */
    RoleDataPermissionDTO.RolePermissionConfig getRolePermissionConfigV2(Long roleId);

    /**
     * V2: 保存角色的数据权限配置
     * @param roleId 角色ID
     * @param config 完整权限配置
     */
    void saveRolePermissionConfigV2(Long roleId, RoleDataPermissionDTO.RolePermissionConfig config);

    /**
     * V2: 获取所有DDD对齐的模块列表
     * @return 按领域分组的模块Map
     */
    Map<String, List<Map<String, String>>> getAllModulesV2();

    /**
     * V2: 获取所有数据范围选项
     * @return 数据范围列表
     */
    List<Map<String, String>> getAllScopesV2();
}
