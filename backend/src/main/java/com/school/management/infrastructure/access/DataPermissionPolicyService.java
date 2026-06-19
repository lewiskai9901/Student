package com.school.management.infrastructure.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.ScopePreset;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.domain.access.model.valueobject.RelationGrant;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限策略服务 —— 读/存 {@code role_data_scopes} 的可组合范围轴。
 *
 * <p>存储真相是三组正交轴 (T9 已删旧 {@code scope_type}/{@code custom_org_unit_ids} 列):
 * <ul>
 *   <li>轴① 组织锚点: {@code org_anchor} / {@code anchor_param} / {@code include_subtree} / {@code custom_org_ids}</li>
 *   <li>轴② 主体关系过滤: {@code subject_rel_include} / {@code subject_rel_exclude}</li>
 *   <li>轴③ 类型过滤: {@code type_filter}</li>
 * </ul>
 *
 * <p>面向 UI 的命名范围码 ({@code scopeCode}: ALL/SELF/DEPARTMENT/.../CUSTOM) 不再持久化,
 * 读路径由轴① 反推 ({@link ScopePreset#scopeCodeFromAxes}); 插件维度直接用 {@code anchor_param}。
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
        String sql = "SELECT apply_to, subject_rel_include, subject_rel_exclude, type_filter, relation_grants " +
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
        List<RelationGrant> grants = parseRelationGrants(row.get("relation_grants"));
        // R3a-2b: 轴① 从 relation_grants 反推 (删 org_anchor 等列后唯一来源)。引擎本身用 grants 出 SQL;
        // 此处轴① 仅供拦截器既有检查 (isOrgUnbounded / PLUGIN_DIM withResourceType) 复用, 避免改拦截器。
        DerivedAxis axis = deriveAxis(grants);

        return ScopeSpec.builder()
                .applyTo(applyTo)
                .orgAnchor(axis.anchor())
                .anchorParam(axis.param())
                .includeSubtree(axis.subtree())
                .customOrgIds(axis.orgIds())
                .subjectRelInclude(parseStringSet(row.get("subject_rel_include")))
                .subjectRelExclude(parseStringSet(row.get("subject_rel_exclude")))
                .typeFilter(parseStringSet(row.get("type_filter")))
                .relationGrants(grants)
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

        // 从 CUSTOM scopeItems 派生组织 id JSON (作为 custom_org_ids 的兜底来源)
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
                log.warn("Failed to serialize custom org ids: {}", e.getMessage());
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

        // R3a-2b: 轴① → relation_grants JSON (写真相源; 与 anchor 列双写, Step B 删列后单写)。
        // isMembership 决定 SELF 落 member-self (owner_org) 还是 creator —— 须与引擎 meta.viaMembership 一致。
        Set<Long> customSet = (spec.getCustomOrgIds() != null && !spec.getCustomOrgIds().isEmpty())
                ? spec.getCustomOrgIds() : parseLongSet(customOrgIdsJson);
        List<RelationGrant> grants = RelationGrant.fromM1Axes(
                spec.getOrgAnchor() != null ? spec.getOrgAnchor() : OrgAnchor.SELF,
                anchorParam, includeSubtree == 1, customSet,
                isMembershipResource(permission.getModuleCode()));
        String relationGrantsJson = toJsonList(grants);

        // UK (role_id, resource_code, apply_to, tenant_id) 不含 deleted, 软删后再 INSERT 会撞 unique.
        // 用 ON DUPLICATE KEY 覆盖同一行, 顺便把 deleted 翻回 0。
        // T9: 只写可组合轴列 (scope_type/custom_org_unit_ids 列已删)。
        jdbcTemplate.update(
                "INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, apply_to, " +
                "org_anchor, anchor_param, include_subtree, custom_org_ids, subject_rel_include, subject_rel_exclude, " +
                "type_filter, relation_grants, priority, created_at, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0) " +
                "ON DUPLICATE KEY UPDATE " +
                "org_anchor=VALUES(org_anchor), anchor_param=VALUES(anchor_param), include_subtree=VALUES(include_subtree), " +
                "custom_org_ids=VALUES(custom_org_ids), subject_rel_include=VALUES(subject_rel_include), " +
                "subject_rel_exclude=VALUES(subject_rel_exclude), " +
                "type_filter=VALUES(type_filter), relation_grants=VALUES(relation_grants), priority=VALUES(priority), updated_at=NOW(), deleted=0",
                tenantId, permission.getRoleId(), permission.getModuleCode(), applyTo,
                orgAnchor, anchorParam, includeSubtree, customOrgIdsJson, subjectRelIncludeJson, subjectRelExcludeJson,
                typeFilterFinal, relationGrantsJson,
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
        String sql = "SELECT id, role_id, resource_code, type_filter, " +
                "apply_to, subject_rel_include, subject_rel_exclude, relation_grants " +
                "FROM role_data_scopes WHERE tenant_id = ? AND role_id = ? AND deleted = 0";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId, roleId);
        return rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Data permission policy cache cleared");
    }

    // ══════════════════════════════════════════════════════════════
    // 内部映射
    // ══════════════════════════════════════════════════════════════

    private RoleDataPermission mapToPermission(Map<String, Object> row) {
        // R3a-2b: 轴① 从 relation_grants 反推 (删 org_anchor 等列后唯一来源)。
        List<RelationGrant> grants = parseRelationGrants(row.get("relation_grants"));
        DerivedAxis axis = deriveAxis(grants);
        OrgAnchor anchor = axis.anchor();
        String anchorParam = axis.param();
        boolean includeSubtree = axis.subtree();
        Set<Long> customOrgIds = axis.orgIds();

        // scope_type 列已删 → 由轴① 反推面向 UI 的命名范围码 (回填前端)。
        // 预设命中 → 预设名 (ALL/SELF/DEPARTMENT/.../CUSTOM); 插件维度 → 维度码 (anchorParam)。
        String scopeCode = ScopePreset.scopeCodeFromAxes(anchor, anchorParam, includeSubtree);
        if (scopeCode == null && anchor == OrgAnchor.PLUGIN_DIM) {
            scopeCode = anchorParam;
        }

        RoleDataPermission permission = RoleDataPermission.builder()
                .id(getLongValue(row, "id"))
                .roleId(getLongValue(row, "role_id"))
                .moduleCode((String) row.get("resource_code"))  // v3 字段映射
                .scopeCode(scopeCode)
                .typeFilter(parseTypeFilter(row.get("type_filter")))
                .description(null)  // v3 无 description 字段
                .applyTo((String) row.get("apply_to"))
                .orgAnchor(anchor)
                .anchorParam(anchorParam)
                .includeSubtree(includeSubtree)
                .customOrgIds(customOrgIds)
                .subjectRelInclude(parseStringSet(row.get("subject_rel_include")))
                .subjectRelExclude(parseStringSet(row.get("subject_rel_exclude")))
                .build();

        // CUSTOM: 由 custom_org_ids 还原 scopeItems (前端 CUSTOM picker 回填用)
        if (anchor == OrgAnchor.CUSTOM_ORG && customOrgIds != null && !customOrgIds.isEmpty()) {
            List<DataScopeItem> items = customOrgIds.stream()
                    .map(id -> DataScopeItem.builder()
                            .roleDataPermissionId(permission.getId())
                            .itemTypeCode("ORG_UNIT")
                            .scopeId(id)
                            .scopeName(null)        // 按需加载
                            .includeChildren(true)  // 默认含子树
                            .build())
                    .collect(Collectors.toList());
            permission.setScopeItems(items);
        }

        return permission;
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

    /**
     * 解析 relation_grants JSON 数组列 → {@code List<RelationGrant>} (R3)。
     * null/空/解析失败 → null —— 调用方 (ScopeEvaluator) 由轴① bridge 兜底, fail-safe 不放宽。
     */
    private List<RelationGrant> parseRelationGrants(Object json) {
        if (json == null) return null;
        String raw = json.toString();
        if (raw.isBlank()) return null;
        try {
            List<RelationGrant> list = objectMapper.readValue(raw, new TypeReference<List<RelationGrant>>() {});
            return (list == null || list.isEmpty()) ? null : list;
        } catch (Exception e) {
            log.warn("Failed to parse relation_grants JSON: {}", e.getMessage());
            return null;
        }
    }

    /** 从 grants 反推的轴① 值 (R3a-2b: 删 org_anchor 等列后, 喂拦截器既有检查 + UI scopeCode)。 */
    private record DerivedAxis(OrgAnchor anchor, String param, boolean subtree, Set<Long> orgIds) {}

    /**
     * 从 relation_grants 反推轴① (单 grant; R3a 现全单 grant, 多 grant 取第一条)。
     * grants 空 → 收窄默认 SELF (不放宽)。供拦截器 isOrgUnbounded/PLUGIN_DIM 检查 + UI scopeCode 复用。
     */
    private DerivedAxis deriveAxis(List<RelationGrant> grants) {
        if (grants == null || grants.isEmpty()) {
            return new DerivedAxis(OrgAnchor.SELF, null, false, null);
        }
        RelationGrant g0 = grants.get(0);
        return new DerivedAxis(g0.anchorOf(), g0.subjectParam(), g0.subtree(), g0.orgIds());
    }

    /** 资源是否成员型 (owner_org=SUBJECT_GRAPH, 如 student/user) —— 决 SELF 落 member-self/creator。 */
    private boolean isMembershipResource(String resourceCode) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resource_relations WHERE resource_code = ? " +
                "AND relation_code = 'owner_org' AND storage_kind = 'SUBJECT_GRAPH' AND enabled = 1",
                Integer.class, resourceCode);
        return cnt != null && cnt > 0;
    }

    /** 序列化 grants 列表为 JSON; null/空 → null (列写 NULL)。 */
    private String toJsonList(List<RelationGrant> grants) {
        if (grants == null || grants.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(grants);
        } catch (Exception e) {
            log.warn("Failed to serialize relation_grants: {}", e.getMessage());
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

    /**
     * 解析 type_filter JSON (闸2/2b) → 类型码列表; null/空/解析失败 → null (= 不过滤)。
     * 委托 {@link #parseStringSet} 复用同一段 JSON 解析, 再转回有序 List。
     */
    private List<String> parseTypeFilter(Object json) {
        Set<String> set = parseStringSet(json);
        return (set == null) ? null : new ArrayList<>(set);
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
