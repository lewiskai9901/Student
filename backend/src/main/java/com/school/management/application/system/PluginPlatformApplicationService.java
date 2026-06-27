package com.school.management.application.system;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件平台数据访问应用服务.
 *
 * <p>承接 {@code PluginPlatformController} 原本直写 {@code JdbcTemplate} 的全部数据访问逻辑,
 * 使 REST 控制器回归 "HTTP 适配 + 表示装配" 单一职责. SQL / 参数 / 返回结构与原控制器逐字一致.</p>
 */
@Service
@RequiredArgsConstructor
public class PluginPlatformApplicationService {

    private final JdbcTemplate jdbc;

    // ═══════════════ overview ═══════════════

    /** 加载 plugin_packages 全量 (CORE 优先排序). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPluginPackages() {
        return jdbc.queryForList(
            "SELECT industry_code, industry_name, version, depends_on, manifest_class, " +
            "enabled, uninstall_policy, installed_at, last_started_at " +
            "FROM plugin_packages ORDER BY (industry_code='CORE') DESC, industry_code");
    }

    /** entity_type_configs 按 industry 分组计数. */
    @Transactional(readOnly = true)
    public Map<String, Long> countTypesByIndustry() {
        return groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM entity_type_configs WHERE deleted=0 AND plugin_enabled=1 GROUP BY industry");
    }

    /** relation_types 按 industry 分组计数. */
    @Transactional(readOnly = true)
    public Map<String, Long> countRelationsByIndustry() {
        return groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM relation_types WHERE is_enabled=1 AND plugin_enabled=1 GROUP BY industry");
    }

    /** entity_event_types 按 industry 分组计数. */
    @Transactional(readOnly = true)
    public Map<String, Long> countEventsByIndustry() {
        return groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM entity_event_types WHERE deleted=0 AND is_enabled=1 AND plugin_enabled=1 GROUP BY industry");
    }

    /** roles 按 industry 分组计数. */
    @Transactional(readOnly = true)
    public Map<String, Long> countRolesByIndustry() {
        return groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM roles WHERE deleted=0 AND plugin_enabled=1 GROUP BY industry");
    }

    /** permissions 按 industry 分组计数. */
    @Transactional(readOnly = true)
    public Map<String, Long> countPermissionsByIndustry() {
        return groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM permissions WHERE deleted=0 AND plugin_enabled=1 GROUP BY industry");
    }

    /** 启用的数据范围维度数 — 表未就绪兜底 0. */
    @Transactional(readOnly = true)
    public long countEnabledDataScopeDims() {
        try {
            Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM data_scope_dims WHERE is_enabled=1 AND plugin_enabled=1", Long.class);
            return c != null ? c : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    /** 触发点总数 — 表未就绪兜底 0. */
    @Transactional(readOnly = true)
    public long countTriggerPoints() {
        try {
            Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM trigger_points WHERE deleted=0 AND plugin_enabled=1", Long.class);
            return c != null ? c : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    /** 启用的订阅规则数 — 表未就绪兜底 0. */
    @Transactional(readOnly = true)
    public long countSubscriptionRules() {
        try {
            Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM msg_subscription_rules WHERE is_enabled=1 AND deleted=0 AND plugin_enabled=1", Long.class);
            return c != null ? c : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    // ═══════════════ health ═══════════════

    /** 查询单个插件包 (健康卡片) — 不存在抛异常给上层转 error. */
    @Transactional(readOnly = true)
    public Map<String, Object> getPluginPackage(String code) {
        return jdbc.queryForMap(
            "SELECT industry_code, industry_name, version, enabled, installed_at, " +
            "       last_started_at, manifest_class, depends_on " +
            "FROM plugin_packages WHERE industry_code=?", code);
    }

    /** entity_type_configs 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countTypesOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM entity_type_configs WHERE industry=? AND deleted=0 AND plugin_enabled=1", code);
    }

    /** relation_types 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countRelationsOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM relation_types WHERE industry=? AND is_enabled=1 AND plugin_enabled=1", code);
    }

    /** entity_event_types 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countEventsOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM entity_event_types WHERE industry=? AND deleted=0 AND plugin_enabled=1", code);
    }

    /** roles 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countRolesOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM roles WHERE industry=? AND deleted=0 AND plugin_enabled=1", code);
    }

    /** permissions 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countPermissionsOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM permissions WHERE industry=? AND deleted=0 AND plugin_enabled=1", code);
    }

    /** event_triggers 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countEventTriggersOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM event_triggers WHERE industry=? AND deleted=0 AND is_enabled=1 AND plugin_enabled=1", code);
    }

    /** data_scope_dims 计数 (按插件). */
    @Transactional(readOnly = true)
    public long countDataScopesOfPlugin(String code) {
        return safeCount(
            "SELECT COUNT(*) FROM data_scope_dims WHERE industry=? AND is_enabled=1 AND plugin_enabled=1", code);
    }

    /** entity_type_configs 样本 (前 3 条). */
    @Transactional(readOnly = true)
    public List<String> sampleTypesOfPlugin(String code) {
        return queryStringList(
            "SELECT CONCAT(type_code, ' ', COALESCE(type_name, '')) " +
            "FROM entity_type_configs WHERE industry=? AND deleted=0 LIMIT 3", code);
    }

    /** relation_types 样本 (前 3 条). */
    @Transactional(readOnly = true)
    public List<String> sampleRelationsOfPlugin(String code) {
        return queryStringList(
            "SELECT CONCAT(relation_code, ' ', COALESCE(relation_name, '')) " +
            "FROM relation_types WHERE industry=? AND is_enabled=1 LIMIT 3", code);
    }

    /** entity_event_types 样本 (前 3 条). */
    @Transactional(readOnly = true)
    public List<String> sampleEventsOfPlugin(String code) {
        return queryStringList(
            "SELECT CONCAT(type_code, ' ', COALESCE(type_name, '')) " +
            "FROM entity_event_types WHERE industry=? AND deleted=0 LIMIT 3", code);
    }

    /** permissions 样本 (前 3 条). */
    @Transactional(readOnly = true)
    public List<String> samplePermissionsOfPlugin(String code) {
        return queryStringList(
            "SELECT CONCAT(permission_code, ' ', COALESCE(permission_name, '')) " +
            "FROM permissions WHERE industry=? AND deleted=0 LIMIT 3", code);
    }

    /** roles 样本 (前 3 条). */
    @Transactional(readOnly = true)
    public List<String> sampleRolesOfPlugin(String code) {
        return queryStringList(
            "SELECT CONCAT(role_code, ' ', COALESCE(role_name, '')) " +
            "FROM roles WHERE industry=? AND deleted=0 LIMIT 3", code);
    }

    /** 查询某依赖插件包的 version / enabled — 不存在抛异常给上层转 MISSING. */
    @Transactional(readOnly = true)
    public Map<String, Object> getDependencyPackage(String depCode) {
        return jdbc.queryForMap(
            "SELECT version, enabled FROM plugin_packages WHERE industry_code=?", depCode);
    }

    // ═══════════════ uninstall (级联软删) ═══════════════

    /** 插件包是否存在. */
    @Transactional(readOnly = true)
    public boolean pluginPackageExists(String code) {
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM plugin_packages WHERE industry_code=?",
            Integer.class, code);
        return exists != null && exists > 0;
    }

    // uninstallPlugin 已删除 (P0-H2): 旧实现用 deleted=1/is_enabled=0 软删 8 表, 这些列被各 Registrar
    // 启动期无条件重置 → 编译内插件 uninstall 重启即复活; 且漏清 data_resources。已收敛到
    // PluginLifecycleService.uninstall (持久 plugin_enabled=0, 与 disable 同一张完整表清单)。

    // ═══════════════ dependency-graph ═══════════════

    /** 依赖图所需的插件包字段子集. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPackagesForDependencyGraph() {
        return jdbc.queryForList(
            "SELECT industry_code, industry_name, version, enabled, depends_on " +
            "  FROM plugin_packages ORDER BY (industry_code='CORE') DESC, industry_code");
    }

    // ═══════════════ M5: 触发点 / 订阅规则 ═══════════════

    /** 全部触发点 + 每个上已配置的触发器数. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTriggerPoints() {
        return jdbc.queryForList(
            "SELECT tp.module_code, tp.module_name, tp.point_code, tp.point_name, " +
            "       tp.description, tp.context_schema, tp.is_enabled, tp.sort_order, " +
            "       (SELECT COUNT(*) FROM event_triggers et " +
            "          WHERE et.trigger_point_code = tp.point_code " +
            "            AND et.deleted = 0 AND et.is_enabled = 1) AS trigger_count " +
            "FROM trigger_points tp WHERE tp.deleted = 0 " +
            "ORDER BY tp.module_code, tp.sort_order, tp.point_code");
    }

    /** 全部订阅规则 (target_mode / 匹配条件 / 目标范围). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSubscriptionRules() {
        return jdbc.queryForList(
            "SELECT id, rule_name, event_category, event_type, target_mode, target_config, " +
            "       channel, is_enabled, tenant_id, created_at " +
            "FROM msg_subscription_rules " +
            "WHERE deleted = 0 " +
            "ORDER BY event_category, event_type, id");
    }

    /**
     * 全部数据资源 (data_resources) —— 数据权限"受控资源"清单。
     * 每行 = 一个可配数据范围的资源 (allowed_scopes) + 所属域/插件 (industry) + 类型轴 (type_field)。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDataResources() {
        return jdbc.queryForList(
            "SELECT resource_code, resource_name, domain_code, domain_name, industry, " +
            "       resource_kind, allowed_scopes, type_field, access_resource_type, " +
            "       subject_relation_filterable, plugin_enabled, enabled, sort_order " +
            "FROM data_resources WHERE tenant_id = 1 " +
            "ORDER BY industry, domain_code, sort_order, resource_code");
    }

    /**
     * 全部资源关系 (resource_relations) —— "数据关系": 每个数据资源如何锚定到主体。
     * owner_org/creator/reviewer/inspected 等, 带存储种类 (COLUMN/SUBJECT_GRAPH/RECORD_RELATION/PROVIDER)、
     * 指向主体类型、列名/关系名/subject 列。与"主体关系"(relation_types) 互补。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listResourceRelations() {
        return jdbc.queryForList(
            "SELECT resource_code, relation_code, relation_name, subject_type, cardinality, " +
            "       storage_kind, column_name, type_column, ar_relation, resolver_bean, subject_column, " +
            "       grants_by_default, enforce_insert_scope, auto_fill, industry, enabled " +
            "FROM resource_relations WHERE enabled = 1 AND tenant_id = 1 " +
            "ORDER BY industry, resource_code, relation_code");
    }

    // ═══════════════ 私有 helper (原控制器迁入) ═══════════════

    private Map<String, Long> groupCount(String sql) {
        Map<String, Long> m = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql)) {
            m.put((String) row.get("k"), ((Number) row.get("c")).longValue());
        }
        return m;
    }

    private long safeCount(String sql, Object... args) {
        try {
            Long c = jdbc.queryForObject(sql, Long.class, args);
            return c != null ? c : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private List<String> queryStringList(String sql, Object... args) {
        try {
            return jdbc.queryForList(sql, String.class, args);
        } catch (Exception e) {
            return List.of();
        }
    }
}
