package com.school.management.application.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.infrastructure.access.DataPermissionPolicyService;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.persistence.access.ScopeItemTypePO;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限应用服务（动态化重构）
 * 使用 DynamicModuleService + DataPermissionPolicyService 替代旧的硬编码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionApplicationService {

    private final DynamicModuleService dynamicModuleService;
    private final DataPermissionPolicyService dataPermissionPolicyService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取所有数据模块（按领域分组）
     */
    public List<DomainModulesDTO> getAllModulesGroupedByDomain() {
        Long tenantId = TenantContextHolder.getTenantId();
        Map<String, List<DataModulePO>> grouped = dynamicModuleService.listByDomain(tenantId);

        return grouped.entrySet().stream()
                .map(entry -> {
                    String domainCode = entry.getKey();
                    String domainName = entry.getValue().stream()
                            .findFirst()
                            .map(DataModulePO::getDomainName)
                            .orElse(domainCode);
                    List<ModuleDTO> modules = entry.getValue().stream()
                            .map(m -> new ModuleDTO(m.getModuleCode(), m.getModuleName()))
                            .collect(Collectors.toList());
                    return new DomainModulesDTO(domainCode, domainName, modules);
                })
                .sorted(Comparator.comparing(DomainModulesDTO::getDomainCode))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有数据范围类型
     */
    public List<ScopeTypeDTO> getAllScopeTypes() {
        return Arrays.stream(DataScope.values())
                .sorted(Comparator.comparingInt(DataScope::getLevel).reversed())
                .map(s -> new ScopeTypeDTO(s.getCode(), s.getDisplayName(), s.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有范围项类型
     */
    public List<ScopeItemTypeDTO> getAllScopeItemTypes() {
        Long tenantId = TenantContextHolder.getTenantId();
        return dynamicModuleService.listAllScopeItemTypes(tenantId).stream()
                .map(t -> new ScopeItemTypeDTO(t.getItemTypeCode(), t.getItemTypeName(), t.getSupportChildren()))
                .collect(Collectors.toList());
    }

    /**
     * 搜索自定义范围可选项
     */
    public List<ScopeItemDTO> searchScopeItems(String itemTypeCode, String keyword, int limit) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<ScopeItemTypePO> types = dynamicModuleService.listAllScopeItemTypes(tenantId);
        ScopeItemTypePO config = types.stream()
                .filter(t -> t.getItemTypeCode().equals(itemTypeCode))
                .findFirst()
                .orElse(null);

        if (config == null) {
            return Collections.emptyList();
        }

        // Use parameterized query with sanitized identifiers
        String refTable = sanitizeIdentifier(config.getRefTable());
        String refIdField = sanitizeIdentifier(config.getRefIdField());
        String refNameField = sanitizeIdentifier(config.getRefNameField());

        String sql = String.format(
                "SELECT %s as id, %s as name FROM %s WHERE %s LIKE ? LIMIT ?",
                refIdField, refNameField, refTable, refNameField
        );

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, "%" + keyword + "%", limit);

        return rows.stream()
                .map(row -> new ScopeItemDTO(
                        getLongValue(row, "id"),
                        (String) row.get("name"),
                        itemTypeCode
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取角色的数据权限配置
     */
    public List<RoleModulePermissionDTO> getRoleDataPermissions(Long roleId) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<RoleDataPermission> permissions = dataPermissionPolicyService.getRolePermissions(tenantId, roleId);

        Map<String, RoleDataPermission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(RoleDataPermission::getModuleCode, p -> p, (a, b) -> a));

        List<DataModulePO> allModules = dynamicModuleService.listModules(tenantId);
        return allModules.stream()
                .map(module -> {
                    RoleDataPermission permission = permissionMap.get(module.getModuleCode());
                    if (permission != null) {
                        List<ScopeItemDTO> scopeItems = permission.getScopeItems() != null ?
                                permission.getScopeItems().stream()
                                        .map(item -> new ScopeItemDTO(
                                                item.getScopeId(),
                                                item.getScopeName(),
                                                item.getItemTypeCode(),
                                                item.getIncludeChildren()
                                        ))
                                        .collect(Collectors.toList()) :
                                Collections.emptyList();
                        return new RoleModulePermissionDTO(
                                module.getModuleCode(),
                                module.getModuleName(),
                                module.getDomainCode(),
                                permission.getScopeCode(),
                                scopeItems
                        );
                    } else {
                        return new RoleModulePermissionDTO(
                                module.getModuleCode(),
                                module.getModuleName(),
                                module.getDomainCode(),
                                DataScope.SELF.getCode(),
                                Collections.emptyList()
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 保存角色的数据权限配置
     */
    @Transactional
    public void saveRoleDataPermissions(Long roleId, List<SavePermissionCommand> commands) {
        Long tenantId = TenantContextHolder.getTenantId();

        List<RoleDataPermission> permissions = commands.stream()
                .map(cmd -> {
                    RoleDataPermission permission = RoleDataPermission.builder()
                            .roleId(roleId)
                            .moduleCode(cmd.getModuleCode())
                            .scopeCode(cmd.getScopeCode())
                            .build();

                    if (DataScope.CUSTOM.getCode().equals(cmd.getScopeCode()) && cmd.getScopeItems() != null) {
                        List<DataScopeItem> items = cmd.getScopeItems().stream()
                                .map(item -> DataScopeItem.builder()
                                        .itemTypeCode(item.getItemTypeCode())
                                        .scopeId(item.getScopeId())
                                        .scopeName(item.getScopeName())
                                        .includeChildren(item.getIncludeChildren() != null ? item.getIncludeChildren() : false)
                                        .build())
                                .collect(Collectors.toList());
                        permission.setScopeItems(items);
                    }

                    return permission;
                })
                .collect(Collectors.toList());

        dataPermissionPolicyService.saveRolePermissions(tenantId, roleId, permissions);
        log.info("Saved {} data permissions for role {}", permissions.size(), roleId);
    }

    /**
     * Get all data modules (flat list format for API)
     */
    public Map<String, List<Map<String, String>>> getAllModules() {
        Long tenantId = TenantContextHolder.getTenantId();
        Map<String, List<DataModulePO>> grouped = dynamicModuleService.listByDomain(tenantId);
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

        grouped.forEach((domain, modules) -> {
            List<Map<String, String>> moduleList = modules.stream()
                    .map(m -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("code", m.getModuleCode());
                        map.put("name", m.getModuleName());
                        map.put("domain", m.getDomainCode());
                        return map;
                    })
                    .collect(Collectors.toList());
            result.put(domain, moduleList);
        });

        return result;
    }

    /**
     * Get all data scope options
     */
    public List<Map<String, String>> getAllScopes() {
        return Arrays.stream(DataScope.values())
                .sorted(Comparator.comparingInt(DataScope::getLevel).reversed())
                .map(s -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("code", s.getCode());
                    map.put("name", s.getDisplayName());
                    map.put("level", String.valueOf(s.getLevel()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private String sanitizeIdentifier(String identifier) {
        if (identifier == null) return "id";
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    // ==================== DTO 类 ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DomainModulesDTO {
        private String domainCode;
        private String domainName;
        private List<ModuleDTO> modules;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ModuleDTO {
        private String code;
        private String name;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScopeTypeDTO {
        private String code;
        private String name;
        private String description;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScopeItemTypeDTO {
        private String code;
        private String name;
        private boolean supportChildren;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ScopeItemDTO {
        private Long scopeId;
        private String scopeName;
        private String itemTypeCode;
        private Boolean includeChildren;

        public ScopeItemDTO(Long scopeId, String scopeName, String itemTypeCode) {
            this.scopeId = scopeId;
            this.scopeName = scopeName;
            this.itemTypeCode = itemTypeCode;
            this.includeChildren = false;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RoleModulePermissionDTO {
        private String moduleCode;
        private String moduleName;
        private String domainCode;
        private String scopeCode;
        private List<ScopeItemDTO> scopeItems;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SavePermissionCommand {
        private String moduleCode;
        private String scopeCode;
        private List<ScopeItemDTO> scopeItems;
    }
}
