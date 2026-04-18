package com.school.management.application.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.persistence.access.DataResourceMapper;
import com.school.management.infrastructure.persistence.access.DataResourcePO;
import com.school.management.infrastructure.persistence.access.ScopeItemTypePO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据资源配置服务 (v3: 指向 data_resources 表)
 *
 * v2: 读 data_modules / scope_item_types / module_scope_item_types 三张表
 * v3: 统一读 data_resources。scope_item_types 相关已弃用(v3 CUSTOM 用 JSON 列表)。
 *
 * API 保持向后兼容: 返回 DataModulePO 结构,调用方无感。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicModuleService {

    private static final String CACHE_NAME = "dynamicModules";

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
        return dataResourceMapper.selectList(
            new LambdaQueryWrapper<DataResourcePO>()
                .eq(DataResourcePO::getTenantId, tenantId)
                .eq(DataResourcePO::getEnabled, 1)
                .orderByAsc(DataResourcePO::getSortOrder)
        ).stream().map(this::toDataModulePO).collect(Collectors.toList());
    }

    public Map<String, List<DataModulePO>> listByDomain(Long tenantId) {
        return listModules(tenantId).stream()
                .collect(Collectors.groupingBy(
                        m -> m.getDomainCode() != null ? m.getDomainCode() : "CORE",
                        LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * @deprecated v3 CUSTOM scope 直接用 org_unit_id 列表,不再需要 scope_item_types。
     *             保留方法签名兼容 API,总是返回空列表。
     */
    @Deprecated
    @Cacheable(value = CACHE_NAME, key = "'scopeTypes:' + #tenantId + ':' + #moduleCode")
    public List<ScopeItemTypePO> listScopeItemTypes(Long tenantId, String moduleCode) {
        return Collections.emptyList();
    }

    /**
     * @deprecated 同上。
     */
    @Deprecated
    @Cacheable(value = CACHE_NAME, key = "'allScopeTypes:' + #tenantId")
    public List<ScopeItemTypePO> listAllScopeItemTypes(Long tenantId) {
        return Collections.emptyList();
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
     * v3: deleteModule 通过 resource_code 删除。注意 id 已废弃(v2 的 DataModulePO.id 对应 data_modules.id,
     * v3 的 data_resources 主键是 resource_code)。调用方仅管理员操作,直接传 resource_code 即可。
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void deleteModule(Long id) {
        log.warn("[DynamicModuleService] deleteModule(id={}) 已废弃: data_resources 主键是 resource_code. 请使用 deleteModuleByCode()", id);
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
        // id: data_resources 主键是 resource_code(字符串),DataModulePO.id 是 Long;
        //     用 hash 生成一个稳定 id 供兼容层使用
        po.setId((long) resource.getResourceCode().hashCode());
        po.setTenantId(resource.getTenantId());
        po.setModuleCode(resource.getResourceCode());
        po.setModuleName(resource.getResourceName());
        po.setDomainCode(resource.getDomainCode());
        po.setDomainName(resource.getDomainName());
        // accessResourceType 单独控制"是否走 access_relations 子查询"
        po.setResourceType(resource.getAccessResourceType());
        po.setOrgUnitField(resource.getOrgUnitField());
        po.setCreatorField(resource.getCreatorField());
        po.setSortOrder(resource.getSortOrder());
        po.setEnabled(resource.getEnabled() != null && resource.getEnabled() == 1);
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
