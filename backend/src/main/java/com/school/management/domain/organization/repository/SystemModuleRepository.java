package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.SystemModule;

import java.util.List;
import java.util.Optional;

/**
 * 系统模块仓储接口
 */
public interface SystemModuleRepository {

    /**
     * 根据模块编码查找
     */
    Optional<SystemModule> findByModuleCode(String moduleCode);

    /**
     * 查找所有启用的模块
     */
    List<SystemModule> findAllEnabled();

    /**
     * 查找顶级模块（无父级）
     */
    List<SystemModule> findTopLevelModules();

    /**
     * 根据父级编码查找子模块
     */
    List<SystemModule> findByParentCode(String parentCode);

    /**
     * 查找所有模块（树形结构）
     */
    List<SystemModule> findAllAsTree();

    /**
     * 保存模块
     */
    SystemModule save(SystemModule module);

    /**
     * 批量保存
     */
    List<SystemModule> saveAll(List<SystemModule> modules);
}
