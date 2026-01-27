package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.SystemModule;
import com.school.management.domain.organization.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统模块仓储实现
 */
@Repository
@RequiredArgsConstructor
public class SystemModuleRepositoryImpl implements SystemModuleRepository {

    private final SystemModuleMapper systemModuleMapper;

    @Override
    public Optional<SystemModule> findByModuleCode(String moduleCode) {
        SystemModulePO po = systemModuleMapper.findByModuleCode(moduleCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SystemModule> findAllEnabled() {
        return systemModuleMapper.findAllEnabled().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemModule> findTopLevelModules() {
        return systemModuleMapper.findTopLevelModules().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemModule> findByParentCode(String parentCode) {
        return systemModuleMapper.findByParentCode(parentCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemModule> findAllAsTree() {
        List<SystemModulePO> allModules = systemModuleMapper.findAllEnabled();
        Map<String, SystemModule> moduleMap = new HashMap<>();
        List<SystemModule> rootModules = new ArrayList<>();

        // 先转换为领域对象并建立映射
        for (SystemModulePO po : allModules) {
            SystemModule module = toDomain(po);
            moduleMap.put(module.getModuleCode(), module);
        }

        // 构建树形结构
        for (SystemModulePO po : allModules) {
            SystemModule module = moduleMap.get(po.getModuleCode());
            if (po.getParentCode() == null || po.getParentCode().isEmpty()) {
                rootModules.add(module);
            } else {
                SystemModule parent = moduleMap.get(po.getParentCode());
                if (parent != null) {
                    parent.addChild(module);
                }
            }
        }

        return rootModules;
    }

    @Override
    public SystemModule save(SystemModule module) {
        SystemModulePO po = toPO(module);
        if (po.getId() == null) {
            systemModuleMapper.insert(po);
        } else {
            systemModuleMapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public List<SystemModule> saveAll(List<SystemModule> modules) {
        return modules.stream()
                .map(this::save)
                .collect(Collectors.toList());
    }

    private SystemModule toDomain(SystemModulePO po) {
        return SystemModule.builder()
                .id(po.getId())
                .moduleCode(po.getModuleCode())
                .moduleName(po.getModuleName())
                .moduleDesc(po.getModuleDesc())
                .parentCode(po.getParentCode())
                .icon(po.getIcon())
                .sortOrder(po.getSortOrder())
                .isEnabled(po.getIsEnabled())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private SystemModulePO toPO(SystemModule domain) {
        SystemModulePO po = new SystemModulePO();
        po.setId(domain.getId());
        po.setModuleCode(domain.getModuleCode());
        po.setModuleName(domain.getModuleName());
        po.setModuleDesc(domain.getModuleDesc());
        po.setParentCode(domain.getParentCode());
        po.setIcon(domain.getIcon());
        po.setSortOrder(domain.getSortOrder());
        po.setIsEnabled(domain.getIsEnabled());
        po.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        return po;
    }
}
