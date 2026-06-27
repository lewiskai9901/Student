package com.school.management.application.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.ScopePreset;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.exception.BusinessException;
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
    private final com.school.management.infrastructure.access.ResourceRelationRegistry resourceRelationRegistry;

    /**
     * R3c: 某资源已注册的可锚定关系 (供多 grant 编辑器关系下拉)。数据驱动 — 来自 resource_relations 注册表。
     */
    public List<ResourceRelationOption> getResourceRelations(String module) {
        return resourceRelationRegistry.relationsOf(module).stream()
                .map(r -> new ResourceRelationOption(
                        r.relationCode(), r.storageKind() != null ? r.storageKind().name() : null))
                .collect(Collectors.toList());
    }

    /** R3c 资源关系选项 (relationCode + storageKind)。 */
    public record ResourceRelationOption(String relationCode, String storageKind) {}

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
        // T8: 范围目录改源自 ScopePreset (与 DataScope 同名同 level, 解耦 T9 将删的 DataScope enum)。
        // ScopePreset 无 description 字段 → 用 displayName 充当描述, 不杜撰逐项文案。
        List<ScopeTypeDTO> hardcoded = Arrays.stream(ScopePreset.values())
                .sorted(Comparator.comparingInt(ScopePreset::getLevel).reversed())
                .map(s -> new ScopeTypeDTO(s.name(), s.getDisplayName(), s.getDisplayName(), "CORE"))
                .collect(Collectors.toList());

        List<ScopeTypeDTO> dynamic;
        try {
            dynamic = jdbcTemplate.query(
                "SELECT dim_code, dim_name, description, domain_code " +
                "FROM data_scope_dims " +
                // plugin_enabled=1: 插件禁用时其 scope 维度 (如 EDU 的 BY_CLASS) 不再出现在字典里
                // (PluginLifecycleService.disable 级联置 plugin_enabled=0, 但 is_enabled 不动)
                "WHERE is_enabled = 1 AND plugin_enabled = 1 " +
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
                        return RoleModulePermissionDTO.builder()
                                .moduleCode(module.getModuleCode())
                                .moduleName(module.getModuleName())
                                .domainCode(module.getDomainCode())
                                .scopeCode(permission.getScopeCode())
                                .scopeItems(scopeItems)
                                .typeFilter(permission.getTypeFilter())
                                // 可组合三轴 (T8): 从 RoleDataPermission 透传当前态
                                .orgAnchor(permission.getOrgAnchor() != null ? permission.getOrgAnchor().name() : null)
                                .anchorParam(permission.getAnchorParam())
                                .includeSubtree(permission.isIncludeSubtree())
                                .customOrgIds(permission.getCustomOrgIds() != null
                                        ? new ArrayList<>(permission.getCustomOrgIds()) : null)
                                .subjectRelInclude(permission.getSubjectRelInclude() != null
                                        ? new ArrayList<>(permission.getSubjectRelInclude()) : null)
                                .subjectRelExclude(permission.getSubjectRelExclude() != null
                                        ? new ArrayList<>(permission.getSubjectRelExclude()) : null)
                                // R3/R4: 透传原始多 grant (UI 回填多锚点; 上面三轴是 deriveAxis 首 grant 视图)
                                .relationGrants(permission.getRelationGrants())
                                .build();
                    } else {
                        return RoleModulePermissionDTO.builder()
                                .moduleCode(module.getModuleCode())
                                .moduleName(module.getModuleName())
                                .domainCode(module.getDomainCode())
                                .scopeCode(ScopePreset.SELF.name())
                                .scopeItems(Collections.emptyList())
                                .typeFilter(null)
                                // 无存储配置 → 轴① 默认 SELF (镜像 scopeCode=SELF 默认)
                                .orgAnchor(OrgAnchor.SELF.name())
                                .includeSubtree(false)
                                .build();
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

        // 模块感知范围校验: 每个 scope 必须在该模块的 allowed_scopes 集合内
        // 未配置 allowed_scopes (null) 的模块 → 宽松放行 (兼容旧数据)
        List<DataModulePO> allModules = dynamicModuleService.listModules(tenantId, true);
        Map<String, List<String>> allowedByModule = allModules.stream()
                .filter(m -> m.getAllowedScopes() != null && !m.getAllowedScopes().isEmpty())
                .collect(Collectors.toMap(DataModulePO::getModuleCode, DataModulePO::getAllowedScopes, (a, b) -> a));

        for (SavePermissionCommand cmd : commands) {
            // R3/R4: 多 grant 配置无单一 scopeCode → 跳过 scopeCode 白名单校验
            if (cmd.getRelationGrants() != null && !cmd.getRelationGrants().isEmpty()) continue;
            List<String> allowed = allowedByModule.get(cmd.getModuleCode());
            if (allowed == null || allowed.isEmpty()) continue;
            if (!allowed.contains(cmd.getScopeCode())) {
                String moduleName = allModules.stream()
                        .filter(m -> cmd.getModuleCode().equals(m.getModuleCode()))
                        .findFirst()
                        .map(DataModulePO::getModuleName)
                        .orElse(cmd.getModuleCode());
                throw new BusinessException(String.format(
                        "模块 [%s] 不支持范围 [%s], 可选: %s",
                        moduleName, cmd.getScopeCode(), allowed));
            }
        }

        List<RoleDataPermission> permissions = commands.stream()
                .map(cmd -> {
                    RoleDataPermission.RoleDataPermissionBuilder builder = RoleDataPermission.builder()
                            .roleId(roleId)
                            .moduleCode(cmd.getModuleCode())
                            .scopeCode(cmd.getScopeCode())
                            .typeFilter(cmd.getTypeFilter());

                    // ── 可组合三轴 (T8): 前端显式传入时填充; 缺省 → 由 saveRolePermission 从 scopeCode 翻译 ──
                    if (cmd.getOrgAnchor() != null) {
                        builder.orgAnchor(OrgAnchor.fromCode(cmd.getOrgAnchor()));
                    }
                    if (cmd.getAnchorParam() != null) {
                        builder.anchorParam(cmd.getAnchorParam());
                    }
                    if (cmd.getIncludeSubtree() != null) {
                        builder.includeSubtree(cmd.getIncludeSubtree());
                    }
                    if (cmd.getCustomOrgIds() != null) {
                        builder.customOrgIds(new LinkedHashSet<>(cmd.getCustomOrgIds()));
                    }
                    if (cmd.getSubjectRelInclude() != null) {
                        builder.subjectRelInclude(new LinkedHashSet<>(cmd.getSubjectRelInclude()));
                    }
                    if (cmd.getSubjectRelExclude() != null) {
                        builder.subjectRelExclude(new LinkedHashSet<>(cmd.getSubjectRelExclude()));
                    }
                    // R3/R4: 显式多 grant 配置 → 直存 (saveRolePermission 优先用之, 跳过 scopeCode 派生)
                    if (cmd.getRelationGrants() != null && !cmd.getRelationGrants().isEmpty()) {
                        builder.relationGrants(cmd.getRelationGrants());
                    }

                    RoleDataPermission permission = builder.build();

                    if (ScopePreset.CUSTOM.name().equals(cmd.getScopeCode()) && cmd.getScopeItems() != null) {
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
     * 按角色智能过滤数据模块.
     *
     * 3 层规则 (优先级):
     *   1. SUPER_ADMIN 豁免 — 返全部 28 模块, relevant=all, advanced=[]
     *   2. industry 对齐 — role.industry=EDU → 只允许 CORE+EDU; role.industry=CUSTOM/NULL → 全部;
     *   3. 功能权限反查 — 非 CORE 模块 必须有 role_permissions 的 permission_code 前缀匹配
     *
     * 返回 {relevant: [...], advanced: [...], meta: {...}}.
     */
    public Map<String, Object> getModulesForRole(Long roleId, boolean includeDisabled) {
        Long tenantId = TenantContextHolder.getTenantId();
        List<DataModulePO> all = dynamicModuleService.listModules(tenantId, includeDisabled);
        List<Map<String, Object>> allDto = all.stream()
                .map(this::toFlatMap)
                .collect(Collectors.toList());

        // roleId 为空 — 返兼容路径
        if (roleId == null) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("relevant", allDto);
            out.put("advanced", Collections.emptyList());
            out.put("meta", Map.of("filtered", false));
            return out;
        }

        // 1. 查角色
        Map<String, Object> role;
        try {
            role = jdbcTemplate.queryForMap(
                    "SELECT id, role_code, role_name, role_type, industry " +
                    "FROM roles WHERE id = ? AND deleted = 0",
                    roleId);
        } catch (Exception e) {
            log.warn("[getModulesForRole] role {} not found: {}", roleId, e.getMessage());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("relevant", allDto);
            out.put("advanced", Collections.emptyList());
            out.put("meta", Map.of("filtered", false, "filterRule", "角色不存在, 返全部"));
            return out;
        }

        String roleCode = (String) role.get("role_code");
        String industry = (String) role.get("industry");
        String roleType = (String) role.get("role_type");

        // 2. 规则 1: SUPER_ADMIN 豁免
        if ("SUPER_ADMIN".equals(roleCode)) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("relevant", allDto);
            out.put("advanced", Collections.emptyList());
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("filtered", false);
            meta.put("filterRule", "超管豁免 — 可配置所有模块");
            meta.put("roleIndustry", industry != null ? industry : "CORE");
            out.put("meta", meta);
            return out;
        }

        // 3. industry 对齐集合 (CORE 永远允许)
        Set<String> allowedIndustries = new LinkedHashSet<>();
        allowedIndustries.add("CORE");
        if (industry == null || industry.isBlank() || "CUSTOM".equals(industry)) {
            // CUSTOM/NULL 角色 — 管理员自主选, 所有行业都允许
            allowedIndustries.addAll(Arrays.asList("CORE", "EDU", "CARE", "CUSTOM"));
        } else {
            allowedIndustries.add(industry);
        }

        // 4. 查角色的权限码前缀集合 (permission_code 形如 "student:info:view")
        Set<String> permModulePrefixes = new HashSet<>();
        try {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT DISTINCT p.permission_code FROM role_permissions rp " +
                    "JOIN permissions p ON p.id = rp.permission_id " +
                    "WHERE rp.role_id = ? AND p.deleted = 0 AND p.plugin_enabled = 1",
                    String.class, roleId);
            for (String c : codes) {
                if (c == null || c.isBlank()) continue;
                String first = c.split(":")[0];
                if (!first.isBlank()) permModulePrefixes.add(first);
            }
        } catch (Exception e) {
            log.warn("[getModulesForRole] query perms failed: {}", e.getMessage());
        }

        boolean hasPerms = !permModulePrefixes.isEmpty();

        // 5. 分流
        List<Map<String, Object>> relevant = new ArrayList<>();
        List<Map<String, Object>> advanced = new ArrayList<>();

        for (Map<String, Object> m : allDto) {
            String mInd = Optional.ofNullable((String) m.get("industry")).orElse("CORE");
            String mCode = (String) m.get("moduleCode");

            boolean industryOk = allowedIndustries.contains(mInd);

            // permOk 逻辑:
            //   - CORE 模块 → 永远保留 (通用)
            //   - 非 CORE 且 role 无任何 permission → 降级: 只按 industry 决定 (给默认权限留出显示)
            //   - 非 CORE 且 role 有 permissions → 必须有前缀匹配
            boolean permOk;
            if ("CORE".equals(mInd)) {
                permOk = true;
            } else if (!hasPerms) {
                // 未配置权限的角色, 不再二次过滤, 按 industry 判断
                permOk = true;
            } else {
                permOk = permModulePrefixes.stream().anyMatch(p ->
                        mCode.equals(p)
                        || mCode.startsWith(p + "_")
                        || mCode.startsWith(p)
                        || p.startsWith(mCode));
            }

            if (industryOk && permOk) {
                relevant.add(m);
            } else {
                Map<String, Object> advCopy = new LinkedHashMap<>(m);
                String reason;
                if (!industryOk) reason = "跨行业模块 (" + mInd + "), 默认隐藏";
                else reason = "角色无 " + mCode + " 相关功能权限";
                advCopy.put("reason", reason);
                advanced.add(advCopy);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("relevant", relevant);
        out.put("advanced", advanced);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("filtered", true);
        String ruleDesc = "industry=" + (industry != null ? industry : "NONE");
        if (hasPerms) ruleDesc += " + perms(" + permModulePrefixes.size() + ")";
        else ruleDesc += " + (未绑权限, 仅按 industry 过滤)";
        meta.put("filterRule", ruleDesc);
        meta.put("totalRelevant", relevant.size());
        meta.put("totalAdvanced", advanced.size());
        meta.put("roleIndustry", industry != null ? industry : "NONE");
        meta.put("roleType", roleType != null ? roleType : "");
        meta.put("rolePermModules", new ArrayList<>(permModulePrefixes));
        out.put("meta", meta);
        return out;
    }

    /** 把 DataModulePO 平铺成 Map (前端契约对齐 DataModuleDTO) */
    private Map<String, Object> toFlatMap(DataModulePO m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("tenantId", m.getTenantId());
        map.put("moduleCode", m.getModuleCode());
        map.put("moduleName", m.getModuleName());
        map.put("domainCode", m.getDomainCode());
        map.put("domainName", m.getDomainName());
        map.put("industry", m.getIndustry() != null ? m.getIndustry() : "CORE");
        map.put("resourceType", m.getResourceType());
        // 类型过滤(闸2/2b): 非空表示该资源支持"按类型过滤", typeEntity 指明可选类型来源
        map.put("typeField", m.getTypeField());
        map.put("typeEntity", m.getTypeEntity());
        // 轴②结果关系过滤能力: true 表示该资源(成员型, 如 user)支持"按结果与用户的关系过滤"
        map.put("relationFilterable", Boolean.TRUE.equals(m.getSubjectRelationFilterable()));
        map.put("sortOrder", m.getSortOrder());
        map.put("enabled", Boolean.TRUE.equals(m.getEnabled()));
        map.put("pluginEnabled", m.getPluginEnabled() == null || m.getPluginEnabled());
        // 本模块支持的 scope 代码数组 (null = 前端用默认全集)
        map.put("allowedScopes", m.getAllowedScopes());
        return map;
    }

    /**
     * Get all data scope options
     */
    public List<Map<String, String>> getAllScopes() {
        // T8: 源自 ScopePreset (解耦 DataScope)
        return Arrays.stream(ScopePreset.values())
                .sorted(Comparator.comparingInt(ScopePreset::getLevel).reversed())
                .map(s -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("code", s.name());
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
        /** 行业 code (CORE / EDU / CARE / CUSTOM) — 一级分组 */
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
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoleModulePermissionDTO {
        private String moduleCode;
        private String moduleName;
        private String domainCode;
        /** 旧预设码 (向后兼容前端旧渲染路径)。 */
        private String scopeCode;
        private List<ScopeItemDTO> scopeItems;
        /** 类型过滤(闸2/2b)=轴③: 已配置的类型码集; null/空=不限 */
        private List<String> typeFilter;

        // ── 可组合三轴 (T8): 供前端范围生成器渲染当前态 ──
        /** 轴① org anchor 名 (ALL/SELF/PRIMARY_ORG/RELATION/CUSTOM_ORG/PLUGIN_DIM); 无配置默认 SELF。 */
        private String orgAnchor;
        /** 轴① 参数: RELATION 时为关系码, PLUGIN_DIM 时为维度码。 */
        private String anchorParam;
        /** 轴① 是否含子树。 */
        private Boolean includeSubtree;
        /** 轴① CUSTOM_ORG 时的指定组织 id 集合。 */
        private List<Long> customOrgIds;
        /** 轴② 结果关系 include。 */
        private List<String> subjectRelInclude;
        /** 轴② 结果关系 exclude。 */
        private List<String> subjectRelExclude;
        /** R3/R4: 原始关系授予数组 (多锚点)。上面三轴是其 deriveAxis 首 grant 视图; 多 grant 配置以此为准。 */
        private List<com.school.management.domain.access.model.valueobject.RelationGrant> relationGrants;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SavePermissionCommand {
        private String moduleCode;
        /** 旧预设码 (ALL/SELF/DEPARTMENT/.../CUSTOM); 仍兼容接收, 未显式带三轴时由其翻译。 */
        private String scopeCode;
        private List<ScopeItemDTO> scopeItems;
        /** 类型过滤(闸2/2b)=轴③: 类型码集, 与组织范围 AND 组合; null/空=不限 */
        private List<String> typeFilter;

        // ── 可组合三轴 (T8): 前端显式传入时持久化; 缺省时由 scopeCode 翻译 (T5 saveRolePermission) ──
        /** 轴① org anchor 名 (ALL/SELF/PRIMARY_ORG/RELATION/CUSTOM_ORG/PLUGIN_DIM)。 */
        private String orgAnchor;
        /** 轴① 参数: RELATION 时为关系码, PLUGIN_DIM 时为维度码。 */
        private String anchorParam;
        /** 轴① 是否含子树。 */
        private Boolean includeSubtree;
        /** 轴① CUSTOM_ORG 时的指定组织 id 集合。 */
        private List<Long> customOrgIds;
        /** 轴② 结果关系 include。 */
        private List<String> subjectRelInclude;
        /** 轴② 结果关系 exclude。 */
        private List<String> subjectRelExclude;
        /** R3/R4: 显式关系授予数组 (多锚点)。非空时直存为 relation_grants, 跳过 scopeCode 派生。 */
        private List<com.school.management.domain.access.model.valueobject.RelationGrant> relationGrants;

        public SavePermissionCommand(String moduleCode, String scopeCode, List<ScopeItemDTO> scopeItems) {
            this(moduleCode, scopeCode, scopeItems, null);
        }

        public SavePermissionCommand(String moduleCode, String scopeCode, List<ScopeItemDTO> scopeItems,
                                     List<String> typeFilter) {
            this.moduleCode = moduleCode;
            this.scopeCode = scopeCode;
            this.scopeItems = scopeItems;
            this.typeFilter = typeFilter;
        }
    }
}
