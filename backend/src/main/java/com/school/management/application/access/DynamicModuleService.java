package com.school.management.application.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.persistence.access.DataResourceMapper;
import com.school.management.infrastructure.persistence.access.DataResourcePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据资源配置服务 (v3)
 * 读写 data_resources 表,返回 DataModulePO 结构(API 契约对象)。
 * CUSTOM scope 在 role_data_scopes.custom_org_unit_ids JSON 列中表达,
 * 不再需要 v2 的 scope_item_types / module_scope_item_types 配置表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicModuleService {

    private static final String CACHE_NAME = "dynamicModules";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<String>> STR_LIST = new TypeReference<List<String>>() {};

    private final DataResourceMapper dataResourceMapper;

    // ==================== Query ====================

    @Cacheable(value = CACHE_NAME, key = "'module:' + #tenantId + ':' + #moduleCode")
    public DataModulePO getModuleConfig(Long tenantId, String moduleCode) {
        DataResourcePO resource = dataResourceMapper.selectOne(
            new LambdaQueryWrapper<DataResourcePO>()
                .eq(DataResourcePO::getTenantId, tenantId)
                .eq(DataResourcePO::getResourceCode, moduleCode)
                .eq(DataResourcePO::getEnabled, 1)
        );
        return resource != null ? toDataModulePO(resource) : null;
    }

    @Cacheable(value = CACHE_NAME, key = "'allModules:' + #tenantId")
    public List<DataModulePO> listModules(Long tenantId) {
        return listModules(tenantId, false);
    }

    /**
     * 列出所有模块.
     * @param includeDisabled 是否包含 plugin_enabled=0 的禁用插件贡献模块.
     *                        false (默认) = 只返启用模块;
     *                        true = 返回全部, DTO 带 pluginEnabled 字段供前端灰显.
     */
    public List<DataModulePO> listModules(Long tenantId, boolean includeDisabled) {
        LambdaQueryWrapper<DataResourcePO> wrapper = new LambdaQueryWrapper<DataResourcePO>()
                .eq(DataResourcePO::getTenantId, tenantId)
                .eq(DataResourcePO::getEnabled, 1)
                .orderByAsc(DataResourcePO::getSortOrder);
        if (!includeDisabled) {
            wrapper.eq(DataResourcePO::getPluginEnabled, true);
        }
        return dataResourceMapper.selectList(wrapper)
                .stream().map(this::toDataModulePO).collect(Collectors.toList());
    }

    public Map<String, List<DataModulePO>> listByDomain(Long tenantId) {
        return listByDomain(tenantId, false);
    }

    public Map<String, List<DataModulePO>> listByDomain(Long tenantId, boolean includeDisabled) {
        return listModules(tenantId, includeDisabled).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getDomainCode() != null ? m.getDomainCode() : "CORE",
                        LinkedHashMap::new, Collectors.toList()));
    }

    // ==================== CRUD ====================

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public DataModulePO createModule(DataModulePO module) {
        DataResourcePO resource = fromDataModulePO(module);
        dataResourceMapper.insert(resource);
        return toDataModulePO(resource);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public DataModulePO updateModule(DataModulePO module) {
        DataResourcePO resource = fromDataModulePO(module);
        dataResourceMapper.updateById(resource);
        return toDataModulePO(resource);
    }

    /**
     * v3: data_resources 主键是 resource_code (VARCHAR),删除通过 resource_code。
     * DataModuleController 传的 Long id 是 DataModulePO.id 的 hash 值,不可用于删除。
     * Controller 已改走按 code 的路径,此方法签名保留供老调用方平滑过渡(实际是 no-op)。
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteModule(Long id) {
        log.warn("[DynamicModuleService] deleteModule(id={}): 使用 deleteModuleByCode(resourceCode) 代替", id);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteModuleByCode(String resourceCode) {
        dataResourceMapper.deleteById(resourceCode);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Dynamic module cache cleared");
    }

    // ==================== Mapper ====================

    private DataModulePO toDataModulePO(DataResourcePO resource) {
        DataModulePO po = new DataModulePO();
        // DataModulePO.id 是 Long, data_resources 主键是 VARCHAR resource_code;
        // 用 hashCode 生成稳定 Long id 供前端/Controller 使用(仅展示,不用于写入)
        po.setId((long) resource.getResourceCode().hashCode());
        po.setTenantId(resource.getTenantId());
        po.setModuleCode(resource.getResourceCode());
        po.setModuleName(resource.getResourceName());
        po.setDomainCode(resource.getDomainCode());
        po.setDomainName(resource.getDomainName());
        po.setIndustry(resource.getIndustry());
        // accessResourceType 单独控制"是否走 access_relations 子查询"
        po.setResourceType(resource.getAccessResourceType());
        po.setOrgUnitField(resource.getOrgUnitField());
        po.setCreatorField(resource.getCreatorField());
        po.setSortOrder(resource.getSortOrder());
        po.setEnabled(resource.getEnabled() != null && resource.getEnabled() == 1);
        // null 视为启用 (DB 默认 1)
        po.setPluginEnabled(resource.getPluginEnabled() == null || resource.getPluginEnabled());
        // 解析 allowed_scopes JSON (null/空 → null, 前端按默认全集渲染)
        String rawScopes = resource.getAllowedScopes();
        if (rawScopes != null && !rawScopes.isBlank()) {
            try {
                po.setAllowedScopes(JSON.readValue(rawScopes, STR_LIST));
            } catch (Exception e) {
                log.warn("[DynamicModuleService] failed to parse allowed_scopes for {}: {}",
                        resource.getResourceCode(), e.getMessage());
            }
        }
        return po;
    }

    private DataResourcePO fromDataModulePO(DataModulePO module) {
        DataResourcePO resource = new DataResourcePO();
        resource.setResourceCode(module.getModuleCode());
        resource.setResourceName(module.getModuleName());
        resource.setDomainCode(module.getDomainCode());
        resource.setDomainName(module.getDomainName());
        resource.setOrgUnitField(module.getOrgUnitField());
        resource.setCreatorField(module.getCreatorField());
        resource.setSortOrder(module.getSortOrder() != null ? module.getSortOrder() : 0);
        resource.setEnabled(Boolean.TRUE.equals(module.getEnabled()) ? 1 : 0);
        resource.setTenantId(module.getTenantId() != null ? module.getTenantId() : 1L);
        resource.setRegisteredBy("MANUAL");
        return resource;
    }
}
