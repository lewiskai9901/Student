package com.school.management.application.organization;

import com.school.management.domain.organization.model.SystemModule;
import com.school.management.domain.organization.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 系统模块应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemModuleService {

    private final SystemModuleRepository systemModuleRepository;

    /**
     * 获取所有启用的模块（平铺列表）
     */
    public List<SystemModule> getAllEnabledModules() {
        return systemModuleRepository.findAllEnabled();
    }

    /**
     * 获取模块树形结构
     */
    public List<SystemModule> getModuleTree() {
        return systemModuleRepository.findAllAsTree();
    }

    /**
     * 获取顶级模块
     */
    public List<SystemModule> getTopLevelModules() {
        return systemModuleRepository.findTopLevelModules();
    }

    /**
     * 根据编码获取模块
     */
    public Optional<SystemModule> getModuleByCode(String moduleCode) {
        return systemModuleRepository.findByModuleCode(moduleCode);
    }

    /**
     * 获取指定模块的子模块
     */
    public List<SystemModule> getChildModules(String parentCode) {
        return systemModuleRepository.findByParentCode(parentCode);
    }
}
