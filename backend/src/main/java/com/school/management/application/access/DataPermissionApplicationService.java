package com.school.management.application.access;

import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.ScopeItemType;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.infrastructure.access.DataModuleRegistry;
import com.school.management.infrastructure.access.DataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限应用服务 (V5)
 * 处理角色数据权限配置的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionApplicationService {

    private final RoleRepository roleRepository;
    private final DataPermissionService dataPermissionService;
    private final DataModuleRegistry moduleRegistry;
    private final JdbcTemplate jdbcTemplate;

    // ==================== V5 API Methods ====================

    /**
     * 获取所有数据模块（按领域分组）
     */
    public List<DomainModulesDTO> getAllModulesGroupedByDomain() {
        Map<String, List<DataModule>> grouped = DataModule.groupByDomain();

        return grouped.entrySet().stream()
                .map(entry -> {
                    String domain = entry.getKey();
                    List<ModuleDTO> modules = entry.getValue().stream()
                            .sorted(Comparator.comparingInt(DataModule::getSortOrder))
                            .map(m -> new ModuleDTO(m.getCode(), m.getName()))
                            .collect(Collectors.toList());
                    return new DomainModulesDTO(domain, DataModule.getDomainDisplayName(domain), modules);
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
        return ScopeItemType.getAllEnabled().stream()
                .map(t -> new ScopeItemTypeDTO(t.getCode(), t.getName(), t.isSupportChildren()))
                .collect(Collectors.toList());
    }

    /**
     * 搜索自定义范围可选项
     */
    public List<ScopeItemDTO> searchScopeItems(String itemTypeCode, String keyword, int limit) {
        ScopeItemType itemType = ScopeItemType.fromCode(itemTypeCode);
        if (itemType == null) {
            return Collections.emptyList();
        }

        DataModuleRegistry.ScopeItemTypeConfig config = moduleRegistry.getScopeItemTypeConfig(itemTypeCode);
        if (config == null) {
            return Collections.emptyList();
        }

        String sql = String.format(
                "SELECT %s as id, %s as name FROM %s WHERE %s LIKE ? LIMIT ?",
                config.getRefIdField(),
                config.getRefNameField(),
                config.getRefTable(),
                config.getRefNameField()
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
     * 获取角色的数据权限配置 (V5)
     */
    public List<RoleModulePermissionDTO> getRoleDataPermissionsV5(Long roleId) {
        List<RoleDataPermission> permissions = dataPermissionService.getRolePermissions(roleId);

        // 构建moduleCode -> permission映射
        Map<String, RoleDataPermission> permissionMap = permissions.stream()
                .collect(Collectors.toMap(RoleDataPermission::getModuleCode, p -> p, (a, b) -> a));

        // 为所有模块构建配置（没有配置的默认SELF）
        return Arrays.stream(DataModule.values())
                .sorted(Comparator.comparingInt(DataModule::getSortOrder))
                .map(module -> {
                    RoleDataPermission permission = permissionMap.get(module.getCode());
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
                                module.getCode(),
                                module.getName(),
                                module.getDomain(),
                                permission.getScopeCode(),
                                scopeItems
                        );
                    } else {
                        return new RoleModulePermissionDTO(
                                module.getCode(),
                                module.getName(),
                                module.getDomain(),
                                DataScope.SELF.getCode(),
                                Collections.emptyList()
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 保存角色的数据权限配置 (V5)
     */
    @Transactional
    public void saveRoleDataPermissionsV5(Long roleId, List<SavePermissionCommand> commands) {
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

        dataPermissionService.saveRolePermissions(roleId, permissions);
        log.info("Saved {} V5 data permissions for role {}", permissions.size(), roleId);
    }

    // ==================== Legacy API Methods (for backward compatibility) ====================

    /**
     * Get all data modules grouped by domain (legacy format).
     */
    public Map<String, List<Map<String, String>>> getAllModules() {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

        Map<String, String> domainLabels = new LinkedHashMap<>();
        domainLabels.put("organization", "组织管理");
        domainLabels.put("space", "场所管理");
        domainLabels.put("inspection", "量化检查");
        domainLabels.put("access", "权限管理");

        for (String domain : domainLabels.keySet()) {
            List<Map<String, String>> modules = Arrays.stream(DataModule.values())
                    .filter(m -> m.getDomain().equals(domain))
                    .sorted(Comparator.comparingInt(DataModule::getSortOrder))
                    .map(m -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("code", m.getCode());
                        map.put("name", m.getName());
                        map.put("domain", m.getDomain());
                        return map;
                    })
                    .collect(Collectors.toList());

            if (!modules.isEmpty()) {
                result.put(domain, modules);
            }
        }

        return result;
    }

    /**
     * Get all data scope options (legacy format).
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

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    // ==================== V5 DTO 类 ====================

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
