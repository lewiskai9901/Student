package com.school.management.application.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.infrastructure.persistence.access.DataModuleMapper;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.persistence.access.ScopeItemTypeMapper;
import com.school.management.infrastructure.persistence.access.ScopeItemTypePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Application service for dynamic module configuration.
 * Replaces hardcoded DataModule enum and DataModuleRegistry.
 * Reads configuration from data_modules and scope_item_types tables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicModuleService {

    private static final String CACHE_NAME = "dynamicModules";

    private final DataModuleMapper dataModuleMapper;
    private final ScopeItemTypeMapper scopeItemTypeMapper;

    // ==================== Query ====================

    @Cacheable(value = CACHE_NAME, key = "'module:' + #tenantId + ':' + #moduleCode")
    public DataModulePO getModuleConfig(Long tenantId, String moduleCode) {
        return dataModuleMapper.selectOne(
            new LambdaQueryWrapper<DataModulePO>()
                .eq(DataModulePO::getTenantId, tenantId)
                .eq(DataModulePO::getModuleCode, moduleCode)
                .eq(DataModulePO::getEnabled, true)
        );
    }

    @Cacheable(value = CACHE_NAME, key = "'allModules:' + #tenantId")
    public List<DataModulePO> listModules(Long tenantId) {
        return dataModuleMapper.selectList(
            new LambdaQueryWrapper<DataModulePO>()
                .eq(DataModulePO::getTenantId, tenantId)
                .eq(DataModulePO::getEnabled, true)
                .orderByAsc(DataModulePO::getSortOrder)
        );
    }

    public Map<String, List<DataModulePO>> listByDomain(Long tenantId) {
        return listModules(tenantId).stream()
                .collect(Collectors.groupingBy(DataModulePO::getDomainCode,
                        LinkedHashMap::new, Collectors.toList()));
    }

    @Cacheable(value = CACHE_NAME, key = "'scopeTypes:' + #tenantId + ':' + #moduleCode")
    public List<ScopeItemTypePO> listScopeItemTypes(Long tenantId, String moduleCode) {
        List<String> typeCodes = dataModuleMapper.findScopeItemTypesByModule(tenantId, moduleCode);
        if (typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return scopeItemTypeMapper.selectList(
            new LambdaQueryWrapper<ScopeItemTypePO>()
                .eq(ScopeItemTypePO::getTenantId, tenantId)
                .in(ScopeItemTypePO::getItemTypeCode, typeCodes)
                .orderByAsc(ScopeItemTypePO::getSortOrder)
        );
    }

    @Cacheable(value = CACHE_NAME, key = "'allScopeTypes:' + #tenantId")
    public List<ScopeItemTypePO> listAllScopeItemTypes(Long tenantId) {
        return scopeItemTypeMapper.selectList(
            new LambdaQueryWrapper<ScopeItemTypePO>()
                .eq(ScopeItemTypePO::getTenantId, tenantId)
                .orderByAsc(ScopeItemTypePO::getSortOrder)
        );
    }

    // ==================== CRUD ====================

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public DataModulePO createModule(DataModulePO module) {
        dataModuleMapper.insert(module);
        return module;
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public DataModulePO updateModule(DataModulePO module) {
        dataModuleMapper.updateById(module);
        return module;
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteModule(Long id) {
        dataModuleMapper.deleteById(id);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Dynamic module cache cleared");
    }
}
