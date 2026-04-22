package com.school.management.application.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.infrastructure.access.DataPermissionPolicyService;
import com.school.management.infrastructure.persistence.access.DataModulePO;
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
     * 获取所有数据模块（按领域分组）.
     * 默认只返启用的 (plugin_enabled=1).
     */
    public List<DomainModulesDTO> getAllModulesGroupedByDomain() {
        return getAllModulesGroupedByDomain(false);
    }

    /**
     * 获取所有数据模块（按领域分组）.
     * @param includeDisabled true = 连同 plugin_enabled=0 的模块一起返回 (DTO 带 pluginEnabled 字段).
     *                        对话框配置页需要 true, 以便把禁用模块灰显并引导启用插件.
     */
    public List<DomainModulesDTO> getAllModulesGroupedByDomain(boolean includeDisabled) {
        Long tenantId = TenantContextHolder.getTenantId();
        Map<String, List<DataModulePO>> grouped = dynamicModuleService.listByDomain(tenantId, includeDisabled);

        return grouped.entrySet().stream()
                .map(entry -> {
                    String domainCode = entry.getKey();
                    String domainName = entry.getValue().stream()
                            .findFirst()
                            .map(DataModulePO::getDomainName)
                            .orElse(domainCode);
                    List<ModuleDTO> modules = entry.getValue().stream()
                            .map(m -> new ModuleDTO(
                                    m.getModuleCode(),
                                    m.getModuleName(),
                                    m.getDomainCode(),
                                    m.getIndustry(),
                                    m.getPluginEnabled() == null || m.getPluginEnabled()
                            ))
                            .collect(Collectors.toList());
                    return new DomainModulesDTO(domainCode, domainName, modules);
                })
                .sorted(Comparator.comparing(DomainModulesDTO::getDomainCode))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有数据范围类型.
     *
     * 返回: core hardcoded 5 种 (source="CORE") + data_scope_dims 表里 is_enabled=1 的插件维度
     * (source="PLUGIN:<domainCode>", 如 PLUGIN:education 的 BY_MAJOR/BY_GRADE/BY_CLASS).
     *
     * ScopeTypeDTO 新增 source 字段, 前端 Vue 对未知字段宽容, 不会 break 现有调用方.
     */
    public List<ScopeTypeDTO> getAllScopeTypes() {
        List<ScopeTypeDTO> hardcoded = Arrays.stream(DataScope.values())
                .sorted(Comparator.comparingInt(DataScope::getLevel).reversed())
                .map(s -> new ScopeTypeDTO(s.getCode(), s.getDisplayName(), s.getDescription(), "CORE"))
                .collect(Collectors.toList());

        List<ScopeTypeDTO> dynamic;
        try {
            dynamic = jdbcTemplate.query(
                "SELECT dim_code, dim_name, description, domain_code " +
                "FROM data_scope_dims " +
                "WHERE is_enabled = 1 " +
                "ORDER BY dim_code",
                (rs, i) -> new ScopeTypeDTO(
                    rs.getString("dim_code"),
                    rs.getString("dim_name"),
                    rs.getString("description"),
                    "PLUGIN:" + Optional.ofNullable(rs.getString("domain_code")).orElse("UNKNOWN")
                ));
        } catch (Exception e) {
            // 表不存在 / 查询失败 -> 只返 core, 不 break API
            log.warn("[DataPermissionApplicationService] failed to query data_scope_dims: {}", e.getMessage());
            dynamic = Collections.emptyList();
        }

        List<ScopeTypeDTO> combined = new ArrayList<>(hardcoded.size() + dynamic.size());
        combined.addAll(hardcoded);
        combined.addAll(dynamic);
        return combined;
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
     * Get all data modules (flat list format for API).
     * 默认只返启用模块.
     */
    public Map<String, List<Map<String, String>>> getAllModules() {
        return getAllModules(false);
    }

    public Map<String, List<Map<String, String>>> getAllModules(boolean includeDisabled) {
        Long tenantId = TenantContextHolder.getTenantId();
        Map<String, List<DataModulePO>> grouped = dynamicModuleService.listByDomain(tenantId, includeDisabled);
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

        grouped.forEach((domain, modules) -> {
            List<Map<String, String>> moduleList = modules.stream()
                    .map(m -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("code", m.getModuleCode());
                        map.put("name", m.getModuleName());
                        map.put("domain", m.getDomainCode());
                        map.put("industry", m.getIndustry() != null ? m.getIndustry() : "CUSTOM");
                        map.put("pluginEnabled", String.valueOf(
                                m.getPluginEnabled() == null || m.getPluginEnabled()));
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
    @lombok.NoArgsConstructor
    public static class ModuleDTO {
        private String code;
        private String name;
        /** 领域 code (CORE / education / inspection ...) — 保留为二级标签 */
        private String domainCode;
        /** 行业 code (CORE / EDU / HEALTH / CARE / CUSTOM) — 一级分组 */
        private String industry;
        /** 所属插件是否启用 (false 时前端灰显 + banner) */
        private Boolean pluginEnabled;

        public ModuleDTO(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ScopeTypeDTO {
        private String code;
        private String name;
        private String description;
        /** 来源标识: "CORE" = hardcoded 5 种; "PLUGIN:<domainCode>" = 插件贡献维度 */
        private String source;

        public ScopeTypeDTO(String code, String name, String description) {
            this(code, name, description, "CORE");
        }
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
