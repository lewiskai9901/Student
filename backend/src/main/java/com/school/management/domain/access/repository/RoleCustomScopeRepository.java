package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.RoleCustomScope;

import java.util.List;

/**
 * 角色自定义数据范围仓储接口
 */
public interface RoleCustomScopeRepository {

    /**
     * 根据角色ID和模块代码查询自定义范围
     */
    List<RoleCustomScope> findByRoleIdAndModule(Long roleId, String moduleCode);

    /**
     * 根据角色ID查询所有自定义范围
     */
    List<RoleCustomScope> findByRoleId(Long roleId);

    /**
     * 保存角色的自定义范围配置
     */
    void saveAll(Long roleId, String moduleCode, List<Long> orgUnitIds);

    /**
     * 删除角色指定模块的自定义范围
     */
    void deleteByRoleIdAndModule(Long roleId, String moduleCode);

    /**
     * 删除角色的所有自定义范围
     */
    void deleteByRoleId(Long roleId);
}
