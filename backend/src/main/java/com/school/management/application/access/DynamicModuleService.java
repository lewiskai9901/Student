package com.school.management.application.access;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
