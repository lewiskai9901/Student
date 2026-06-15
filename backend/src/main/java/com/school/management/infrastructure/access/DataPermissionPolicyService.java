package com.school.management.infrastructure.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.ScopePreset;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限策略服务 (v3 改读 role_data_scopes 替代 role_data_permissions_v5)
 *
 * 关键字段映射:
 *   v2: module_code / scope_code        (role_data_permissions_v5)
 *   v3: resource_code / scope_type      (role_data_scopes)
 *
 * CUSTOM scope 自定义项:
 *   v2: role_data_scope_items 独立表 (item_type_code / scope_id / scope_name / include_children)
 *   v3: role_data_scopes.custom_org_unit_ids JSON 列表
 *   简化: v3 CUSTOM 就是"给这个角色指定这些组织 ID",不再区分 item_type_code。
 */
@Slf4j
@Service
public class DataPermissionPolicyService {

    private static final String CACHE_NAME = "dataPermissionPolicy";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DataPermissionPolicyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取多角色合并 DataScope。
     * Cache key 使用 sorted roleIds 避免顺序问题。
     */
    @Cacheable(value = CACHE_NAME,
            key = "'merged:' + #tenantId + ':' + new java.util.TreeSet(#roleIds) + ':' + #moduleCode",
            unless = "#result == null")
    public MergedDataScope getMergedScope(Long tenantId, List<Long> roleIds, String moduleCode) {
        if (roleIds == null || roleIds.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.SELF)
                    .hasSelfScope(true)
                    .build();
        }

        // v3: role_data_scopes (resource_code / scope_type / custom_org_unit_ids / type_filter)
        String placeholders = roleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, role_id, resource_code, scope_type, custom_org_unit_ids, type_filter " +
                "FROM role_data_scopes WHERE tenant_id = ? AND role_id IN (" +
                placeholders + ") AND resource_code = ? AND deleted = 0";

        Object[] params = new Object[roleIds.size() + 2];
        params[0] = tenantId;
        for (int i = 0; i < roleIds.size(); i++) {
            params[i + 1] = roleIds.get(i);
        }
        params[roleIds.size() + 1] = moduleCode;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

        if (rows.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.SELF)
                    .hasSelfScope(true)
                    .sourceRoleIds(roleIds)
                    .build();
        }

        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        // CUSTOM scope: 从 custom_org_unit_ids JSON 解析组织 ID 列表
        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadCustomOrgUnitIds(permission, rows);
            }
        }

        return MergedDataScope.merge(moduleCode, permissions);
    }

    /**
     * 读取可组合数据范围规格 (ScopeSpec) —— 单角色×资源×动作类的一行配置。
     *
     * <p>从新轴列 (org_anchor / anchor_param / include_subtree / custom_org_ids /
     * subject_rel_include / subject_rel_exclude / type_filter / apply_to) 构造 {@link ScopeSpec}。
     *
     * @param actionClass "READ" 或 "WRITE"。命中 {@code apply_to IN (actionClass, 'BOTH')},
     *                    精确动作类优先于 BOTH。
     * @return 该规格; 无配置行 → {@code null} (调用方视为"无配置 → 默认 SELF")。
     */
    public ScopeSpec getScopeSpec(Long tenantId, Long roleId, String resourceCode, String actionClass) {
        // apply_to IN (actionClass, 'BOTH'), 精确动作类优先 (apply_to='BOTH' ASC → 非 BOTH 排前)
        String sql = "SELECT apply_to, org_anchor, anchor_param, include_subtree, " +
                "custom_org_ids, subject_rel_include, subject_rel_exclude, type_filter, scope_type " +
                "FROM role_data_scopes " +
                "WHERE tenant_id = ? AND role_id = ? AND resource_code = ? " +
                "AND apply_to IN (?, 'BOTH') AND deleted = 0 " +
                "ORDER BY (apply_to = 'BOTH') ASC LIMIT 1";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                sql, tenantId, roleId, resourceCode, actionClass);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return mapToScopeSpec(rows.get(0));
    }

    /** 把 role_data_scopes 一行 (新轴列) 映射为 {@link ScopeSpec}。 */
    private ScopeSpec mapToScopeSpec(Map<String, Object> row) {
        String applyTo = (String) row.get("apply_to");
        OrgAnchor anchor = OrgAnchor.fromCode((String) row.get("org_anchor"));
        String anchorParam = (String) row.get("anchor_param");
        boolean includeSubtree = getIntValue(row.get("include_subtree")) == 1;

        if (anchor == null) {
            // 防御: org_anchor 为 NULL (迁移后不应出现) → 用旧 scope_type 翻译
            String scopeType = (String) row.get("scope_type");
            ScopePreset preset = ScopePreset.fromLegacyScopeType(scopeType);
            if (preset != null) {
                ScopeSpec base = preset.toSpec();
                anchor = base.getOrgAnchor();
                anchorParam = base.getAnchorParam();
                includeSubtree = base.isIncludeSubtree();
            } else {
                // 仍未知 → 当作插件维度, anchorParam = scope_type
                anchor = OrgAnchor.PLUGIN_DIM;
                anchorParam = scopeType;
            }
        }

        return ScopeSpec.builder()
                .applyTo(applyTo)
                .orgAnchor(anchor)
                .anchorParam(anchorParam)
                .includeSubtree(includeSubtree)
                .customOrgIds(parseLongSet(row.get("custom_org_ids")))
                .subjectRelInclude(parseStringSet(row.get("subject_rel_include")))
                .subjectRelExclude(parseStringSet(row.get("subject_rel_exclude")))
                .typeFilter(parseStringSet(row.get("type_filter")))
                .build();
    }

    /** 保存单条角色数据权限配置 */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermission(Long tenantId, RoleDataPermission permission) {
        // 软删除旧配置 (同一 role+resource 的旧记录置 deleted=1)
        jdbcTemplate.update(
                "UPDATE role_data_scopes SET deleted = 1 " +
                "WHERE tenant_id = ? AND role_id = ? AND resource_code = ? AND deleted = 0",
                tenantId, permission.getRoleId(), permission.getModuleCode());

        // 构造 custom_org_unit_ids JSON
        String customJson = null;
        if ("CUSTOM".equals(permission.getScopeCode()) && permission.getScopeItems() != null
                && !permission.getScopeItems().isEmpty()) {
            try {
                List<Long> orgIds = permission.getScopeItems().stream()
                        .map(DataScopeItem::getScopeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                customJson = objectMapper.writeValueAsString(orgIds);
            } catch (Exception e) {
                log.warn("Failed to serialize custom_org_unit_ids: {}", e.getMessage());
            }
        }

        // 构造 type_filter JSON (闸2/2b): 空集 → NULL (不过滤)
        String typeFilterJson = null;
        if (permission.getTypeFilter() != null && !permission.getTypeFilter().isEmpty()) {
            try {
                typeFilterJson = objectMapper.writeValueAsString(permission.getTypeFilter());
            } catch (Exception e) {
                log.warn("Failed to serialize type_filter: {}", e.getMessage());
            }
        }

        // ── 可组合轴列 ──
        // 优先用 permission 已显式带的轴 (T8 迁移后上层会填); 否则从 scopeCode 翻译,
        // 这样仍传 scopeCode 的旧调用方也能落到新列。
        ScopeSpec spec = deriveSpecForSave(permission);
        String applyTo = (spec.getApplyTo() != null) ? spec.getApplyTo() : "BOTH";
        String orgAnchor = (spec.getOrgAnchor() != null) ? spec.getOrgAnchor().name() : null;
        String anchorParam = spec.getAnchorParam();
        int includeSubtree = spec.isIncludeSubtree() ? 1 : 0;
        // custom_org_ids: 优先 spec.customOrgIds, 否则复用上面从 scopeItems 派生的 customJson
        String customOrgIdsJson = (spec.getCustomOrgIds() != null && !spec.getCustomOrgIds().isEmpty())
                ? toJson(spec.getCustomOrgIds()) : customJson;
        String subjectRelIncludeJson = toJson(spec.getSubjectRelInclude());
        String subjectRelExcludeJson = toJson(spec.getSubjectRelExclude());
        // type_filter 优先 spec.typeFilter, 否则复用上面从 permission.typeFilter 派生的 typeFilterJson
        String typeFilterFinal = (spec.getTypeFilter() != null && !spec.getTypeFilter().isEmpty())
                ? toJson(spec.getTypeFilter()) : typeFilterJson;

        // UK (role_id, resource_code, apply_to, tenant_id) 不含 deleted, 软删后再 INSERT 会撞 unique.
        // 用 ON DUPLICATE KEY 覆盖同一行, 顺便把 deleted 翻回 0。
        // 同时写新轴列 + 旧 scope_type (T9 删除前保持双写)。
        jdbcTemplate.update(
                "INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, apply_to, scope_type, " +
                "org_anchor, anchor_param, include_subtree, custom_org_ids, subject_rel_include, subject_rel_exclude, " +
                "custom_org_unit_ids, type_filter, priority, created_at, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0) " +
                "ON DUPLICATE KEY UPDATE scope_type=VALUES(scope_type), " +
                "org_anchor=VALUES(org_anchor), anchor_param=VALUES(anchor_param), include_subtree=VALUES(include_subtree), " +
                "custom_org_ids=VALUES(custom_org_ids), subject_rel_include=VALUES(subject_rel_include), " +
                "subject_rel_exclude=VALUES(subject_rel_exclude), custom_org_unit_ids=VALUES(custom_org_unit_ids), " +
                "type_filter=VALUES(type_filter), priority=VALUES(priority), updated_at=NOW(), deleted=0",
                tenantId, permission.getRoleId(), permission.getModuleCode(), applyTo,
                permission.getScopeCode(),
                orgAnchor, anchorParam, includeSubtree, customOrgIdsJson, subjectRelIncludeJson, subjectRelExcludeJson,
                customJson, typeFilterFinal,
                0);

        log.info("Saved role data scope: tenantId={}, roleId={}, resourceCode={}, scopeType={}, anchor={}, applyTo={}",
                tenantId, permission.getRoleId(), permission.getModuleCode(), permission.getScopeCode(), orgAnchor, applyTo);
    }

    /** 批量保存角色的所有数据权限 */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermissions(Long tenantId, Long roleId, List<RoleDataPermission> permissions) {
        jdbcTemplate.update(
                "UPDATE role_data_scopes SET deleted = 1 WHERE tenant_id = ? AND role_id = ? AND deleted = 0",
                tenantId, roleId);
        for (RoleDataPermission permission : permissions) {
            permission.setRoleId(roleId);
            saveRolePermission(tenantId, permission);
        }
    }

    /** 获取角色的所有权限配置 */
    public List<RoleDataPermission> getRolePermissions(Long tenantId, Long roleId) {
        String sql = "SELECT id, role_id, resource_code, scope_type, custom_org_unit_ids, type_filter " +
                "FROM role_data_scopes WHERE tenant_id = ? AND role_id = ? AND deleted = 0";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId, roleId);
        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadCustomOrgUnitIds(permission, rows);
            }
        }

        return permissions;
    }

    /** 获取单角色在某资源上的 DataScope (仅 core hardcoded 5 种). 插件维度返回 null. */
    public DataScope getScopeForRole(Long tenantId, Long roleId, String moduleCode) {
        String scopeCode = getScopeCodeForRole(tenantId, roleId, moduleCode);
        if (scopeCode == null) return null;
        DataScope core = DataScope.fromCodeStrict(scopeCode);
        if (core == null) {
            // 插件维度, 由 interceptor 走 PluginDataScopeRouter 路由
            log.debug("scope_type '{}' is a plugin dim for role {} resource {}", scopeCode, roleId, moduleCode);
        }
        return core;
    }

    /**
     * 获取单角色在某资源上的原始 scope code 字符串.
     *
     * 相比 {@link #getScopeForRole} 这个不做 enum 转换, 调用方可自行判断是否是 core 维度
     * (DataScope.fromCodeStrict) 还是插件维度(走 PluginDataScopeRouter).
     */
    public String getScopeCodeForRole(Long tenantId, Long roleId, String moduleCode) {
        String sql = "SELECT scope_type FROM role_data_scopes " +
                "WHERE tenant_id = ? AND role_id = ? AND resource_code = ? AND deleted = 0 LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, tenantId, roleId, moduleCode);
        } catch (Exception e) {
            // No config found → return null
            return null;
        }
    }

    /**
     * 获取单角色在某资源上的类型过滤集 (闸2/2b).
     *
     * 仅在该资源配置了 type_field 时由拦截器调用 (大多数资源无 type_field → 拦截器根本不调,
     * 零额外开销)。返回的类型码集与组织范围 AND 组合。null/空 = 不做类型过滤。
     */
    public List<String> getTypeFilterForRole(Long tenantId, Long roleId, String moduleCode) {
        String sql = "SELECT type_filter FROM role_data_scopes " +
                "WHERE tenant_id = ? AND role_id = ? AND resource_code = ? AND deleted = 0 LIMIT 1";
        try {
            String json = jdbcTemplate.queryForObject(sql, String.class, tenantId, roleId, moduleCode);
            return parseTypeFilter(json);
        } catch (Exception e) {
            // 无配置 / 查询失败 → 不过滤
            return null;
        }
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Data permission policy cache cleared");
    }

    // ══════════════════════════════════════════════════════════════
    // 内部映射
    // ══════════════════════════════════════════════════════════════

    private RoleDataPermission mapToPermission(Map<String, Object> row) {
        return RoleDataPermission.builder()
                .id(getLongValue(row, "id"))
                .roleId(getLongValue(row, "role_id"))
                .moduleCode((String) row.get("resource_code"))  // v3 字段映射
                .scopeCode((String) row.get("scope_type"))      // v3 字段映射
                .typeFilter(parseTypeFilter(row.get("type_filter")))
                .description(null)  // v3 无 description 字段
                .build();
    }

    /**
     * 为 save 派生 ScopeSpec: 优先用 permission 已显式带的轴; 任一轴缺失时从 scopeCode 翻译补齐。
     * 这样 T8 迁移前仍只传 scopeCode 的旧调用方也能落到新列。
     */
    private ScopeSpec deriveSpecForSave(RoleDataPermission permission) {
        OrgAnchor anchor = permission.getOrgAnchor();
        String anchorParam = permission.getAnchorParam();
        boolean includeSubtree = permission.isIncludeSubtree();

        if (anchor == null) {
            // 从 scopeCode 翻译轴①
            ScopePreset preset = ScopePreset.fromLegacyScopeType(permission.getScopeCode());
            if (preset != null) {
                ScopeSpec base = preset.toSpec();
                anchor = base.getOrgAnchor();
                anchorParam = base.getAnchorParam();
                includeSubtree = base.isIncludeSubtree();
            } else if (permission.getScopeCode() != null) {
                // 非预设 → 插件维度 (BY_CLASS/BY_MAJOR/...), anchorParam = scopeCode
                anchor = OrgAnchor.PLUGIN_DIM;
                anchorParam = permission.getScopeCode();
            }
        }

        Set<String> typeFilterSet = (permission.getTypeFilter() != null)
                ? new LinkedHashSet<>(permission.getTypeFilter()) : null;

        return ScopeSpec.builder()
                .applyTo(permission.getApplyTo())
                .orgAnchor(anchor)
                .anchorParam(anchorParam)
                .includeSubtree(includeSubtree)
                .customOrgIds(permission.getCustomOrgIds())
                .subjectRelInclude(permission.getSubjectRelInclude())
                .subjectRelExclude(permission.getSubjectRelExclude())
                .typeFilter(typeFilterSet)
                .build();
    }

    /** 序列化集合为 JSON; null/空 → null (列写 NULL)。 */
    private String toJson(Set<?> set) {
        if (set == null || set.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(set);
        } catch (Exception e) {
            log.warn("Failed to serialize set to JSON: {}", e.getMessage());
            return null;
        }
    }

    /** 解析 JSON 数组列 → {@code Set<Long>}; null/空/解析失败 → null。 */
    private Set<Long> parseLongSet(Object json) {
        if (json == null) return null;
        String raw = json.toString();
        if (raw.isBlank()) return null;
        try {
            List<Long> list = objectMapper.readValue(raw, new TypeReference<List<Long>>() {});
            return (list == null || list.isEmpty()) ? null : new LinkedHashSet<>(list);
        } catch (Exception e) {
            log.warn("Failed to parse Long set JSON: {}", e.getMessage());
            return null;
        }
    }

    /** 解析 JSON 数组列 → {@code Set<String>}; null/空/解析失败 → null。 */
    private Set<String> parseStringSet(Object json) {
        if (json == null) return null;
        String raw = json.toString();
        if (raw.isBlank()) return null;
        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            return (list == null || list.isEmpty()) ? null : new LinkedHashSet<>(list);
        } catch (Exception e) {
            log.warn("Failed to parse String set JSON: {}", e.getMessage());
            return null;
        }
    }

    /** 取整数值 (列可能是 Boolean/Number/null)。null → 0。 */
    private int getIntValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 解析 type_filter JSON (闸2/2b) → 类型码列表; null/空/解析失败 → null (= 不过滤) */
    private List<String> parseTypeFilter(Object json) {
        if (json == null) return null;
        String raw = json.toString();
        if (raw.isBlank()) return null;
        try {
            List<String> codes = objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            return (codes == null || codes.isEmpty()) ? null : codes;
        } catch (Exception e) {
            log.warn("Failed to parse type_filter: {}", e.getMessage());
            return null;
        }
    }

    /** 从 custom_org_unit_ids JSON 构造 scopeItems 列表 */
    private void loadCustomOrgUnitIds(RoleDataPermission permission, List<Map<String, Object>> allRows) {
        // 找到 permission 对应的行
        Map<String, Object> myRow = allRows.stream()
                .filter(r -> Objects.equals(getLongValue(r, "id"), permission.getId()))
                .findFirst().orElse(null);
        if (myRow == null) return;

        Object json = myRow.get("custom_org_unit_ids");
        if (json == null) return;

        try {
            List<Long> orgIds = objectMapper.readValue(json.toString(),
                    new TypeReference<List<Long>>() {});
            List<DataScopeItem> items = orgIds.stream()
                    .map(id -> DataScopeItem.builder()
                            .roleDataPermissionId(permission.getId())
                            .itemTypeCode("ORG_UNIT")     // v3 无 item_type_code,统一 'ORG_UNIT' (大写, 与 MergedDataScope.getOrgUnitIds/DataScopeItem.isOrgUnitType 对齐)
                            .scopeId(id)
                            .scopeName(null)              // 按需加载
                            .includeChildren(true)        // v3 默认 include_children
                            .build())
                    .collect(Collectors.toList());
            permission.setScopeItems(items);
        } catch (Exception e) {
            log.warn("Failed to parse custom_org_unit_ids: {}", e.getMessage());
        }
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
