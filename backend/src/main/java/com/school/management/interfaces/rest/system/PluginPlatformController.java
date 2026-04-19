package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.PluginManifest;
import com.school.management.infrastructure.extension.PluginPackageRegistrar;
import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 插件平台 REST API — 给 /system/plugins 页面用.
 * 聚合行业包 + 其贡献统计.
 */
@RestController
@RequestMapping("/plugin-platform")
@RequiredArgsConstructor
public class PluginPlatformController {

    private final PluginPackageRegistrar packageRegistrar;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventPublisher;
    private final com.school.management.infrastructure.extension.TenantPluginService tenantPluginService;

    /**
     * GET /api/plugin-platform/overview
     * 返回: {
     *   industries: [{code, name, version, enabled, dependsOn, stats:{types,relations,events,roles,permissions}}],
     *   summary: {types, relations, events, roles, permissions}
     * }
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        // 加载 plugin_packages
        List<Map<String, Object>> packages = jdbc.queryForList(
            "SELECT industry_code, industry_name, version, depends_on, manifest_class, " +
            "enabled, uninstall_policy, installed_at, last_started_at " +
            "FROM plugin_packages ORDER BY (industry_code='CORE') DESC, industry_code");

        // 各表按 industry 分组统计
        Map<String, Long> typeByIndustry = groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM entity_type_configs WHERE deleted=0 GROUP BY industry");
        Map<String, Long> relationByIndustry = groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM relation_types WHERE is_enabled=1 GROUP BY industry");
        Map<String, Long> eventByIndustry = groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM entity_event_types WHERE deleted=0 AND is_enabled=1 GROUP BY industry");
        Map<String, Long> roleByIndustry = groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM roles WHERE deleted=0 GROUP BY industry");
        Map<String, Long> permByIndustry = groupCount(
            "SELECT COALESCE(industry,'UNKNOWN') k, COUNT(*) c FROM permissions WHERE deleted=0 GROUP BY industry");

        // 组装 industries
        List<Map<String, Object>> industries = new ArrayList<>();
        for (Map<String, Object> pkg : packages) {
            String code = (String) pkg.get("industry_code");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", code);
            item.put("name", pkg.get("industry_name"));
            item.put("version", pkg.get("version"));
            item.put("dependsOn", pkg.get("depends_on"));
            item.put("manifestClass", pkg.get("manifest_class"));
            item.put("enabled", ((Number) pkg.get("enabled")).intValue() == 1);
            item.put("uninstallPolicy", pkg.get("uninstall_policy"));
            item.put("installedAt", pkg.get("installed_at"));
            item.put("lastStartedAt", pkg.get("last_started_at"));
            Map<String, Long> stats = new LinkedHashMap<>();
            stats.put("types", typeByIndustry.getOrDefault(code, 0L));
            stats.put("relations", relationByIndustry.getOrDefault(code, 0L));
            stats.put("events", eventByIndustry.getOrDefault(code, 0L));
            stats.put("roles", roleByIndustry.getOrDefault(code, 0L));
            stats.put("permissions", permByIndustry.getOrDefault(code, 0L));
            item.put("stats", stats);
            industries.add(item);
        }

        // 统计总数
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("types", sumOf(typeByIndustry));
        summary.put("relations", sumOf(relationByIndustry));
        summary.put("events", sumOf(eventByIndustry));
        summary.put("roles", sumOf(roleByIndustry));
        summary.put("permissions", sumOf(permByIndustry));
        summary.put("industries", (long) industries.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("industries", industries);
        result.put("summary", summary);
        result.put("loadedManifests",
            packageRegistrar.getSortedManifests().stream()
                .map(PluginManifest::getIndustryCode).toList());
        return Result.success(result);
    }

    private Map<String, Long> groupCount(String sql) {
        Map<String, Long> m = new HashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(sql)) {
            m.put((String) row.get("k"), ((Number) row.get("c")).longValue());
        }
        return m;
    }

    private Long sumOf(Map<String, Long> m) {
        return m.values().stream().mapToLong(Long::longValue).sum();
    }

    // ═══════════════ Phase 6: 生命周期治理 API ═══════════════

    /**
     * POST /api/plugin-platform/{code}/enable
     * 启用插件包. 仅修改 plugin_packages.enabled=1, 下次启动重新扫描数据.
     * 立即刷 Casbin 保证 UI 层生效.
     */
    @PostMapping("/{code}/enable")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> enable(@PathVariable String code) {
        if ("CORE".equalsIgnoreCase(code)) {
            return Result.error("CORE 包不可禁用");
        }
        int n = jdbc.update("UPDATE plugin_packages SET enabled=1 WHERE industry_code=?", code);
        if (n == 0) return Result.error("插件不存在: " + code);
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "ENABLE:" + code));
        return Result.success(Map.of("code", code, "enabled", true, "updated", n));
    }

    /**
     * POST /api/plugin-platform/{code}/disable
     * 禁用插件包. 数据保留但 enabled=0, UI 菜单不再出现.
     */
    @PostMapping("/{code}/disable")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> disable(@PathVariable String code) {
        if ("CORE".equalsIgnoreCase(code)) {
            return Result.error("CORE 包不可禁用");
        }
        int n = jdbc.update("UPDATE plugin_packages SET enabled=0 WHERE industry_code=?", code);
        if (n == 0) return Result.error("插件不存在: " + code);
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "DISABLE:" + code));
        return Result.success(Map.of("code", code, "enabled", false, "updated", n));
    }

    /**
     * GET /api/plugin-platform/{code}/health
     * 返回插件健康状态: 贡献数量 vs Manifest 声明.
     */
    @GetMapping("/{code}/health")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> health(@PathVariable String code) {
        Map<String, Object> pkg = null;
        try {
            pkg = jdbc.queryForMap(
                "SELECT industry_code, industry_name, version, enabled, installed_at, last_started_at " +
                "FROM plugin_packages WHERE industry_code=?", code);
        } catch (Exception e) {
            return Result.error("插件不存在: " + code);
        }

        Long types = jdbc.queryForObject(
            "SELECT COUNT(*) FROM entity_type_configs WHERE industry=? AND deleted=0", Long.class, code);
        Long relations = jdbc.queryForObject(
            "SELECT COUNT(*) FROM relation_types WHERE industry=? AND is_enabled=1", Long.class, code);
        Long events = jdbc.queryForObject(
            "SELECT COUNT(*) FROM entity_event_types WHERE industry=? AND deleted=0", Long.class, code);
        Long roles = jdbc.queryForObject(
            "SELECT COUNT(*) FROM roles WHERE industry=? AND deleted=0", Long.class, code);
        Long permissions = jdbc.queryForObject(
            "SELECT COUNT(*) FROM permissions WHERE industry=? AND deleted=0", Long.class, code);

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("package", pkg);
        health.put("contributions", Map.of(
            "types", types, "relations", relations, "events", events,
            "roles", roles, "permissions", permissions));
        long total = types + relations + events + roles + permissions;
        health.put("status", total > 0 ? "HEALTHY" : "NO_CONTRIBUTIONS");
        health.put("totalContributions", total);
        return Result.success(health);
    }

    /**
     * POST /api/plugin-platform/{code}/uninstall
     * 级联软删本行业所有贡献 (industry=X 的行). CUSTOM 资源不受影响.
     *
     * 影响的表: permissions / roles / entity_type_configs / entity_event_types /
     *           trigger_points / event_triggers / relation_types / data_scope_dims
     *
     * CORE 不可卸载 (核心功能依赖).
     */
    @PostMapping("/{code}/uninstall")
    @CasbinAccess(resource = "admin", action = "access")
    @org.springframework.transaction.annotation.Transactional
    public Result<Map<String, Object>> uninstall(@PathVariable String code) {
        if ("CORE".equalsIgnoreCase(code)) {
            return Result.error("CORE 包不可卸载");
        }
        // 检查插件包存在
        Integer exists = jdbc.queryForObject(
            "SELECT COUNT(*) FROM plugin_packages WHERE industry_code=?",
            Integer.class, code);
        if (exists == null || exists == 0) {
            return Result.error("插件不存在: " + code);
        }

        Map<String, Integer> cascadeCounts = new LinkedHashMap<>();
        // 贡献表列表 (表 -> 软删 SQL)
        java.util.List<String[]> tables = java.util.List.of(
            new String[]{"permissions",          "UPDATE permissions SET deleted=1, status=0 WHERE industry=? AND deleted=0"},
            new String[]{"roles",                "UPDATE roles SET deleted=1, status=0 WHERE industry=? AND deleted=0"},
            new String[]{"entity_type_configs",  "UPDATE entity_type_configs SET deleted=1, is_enabled=0 WHERE industry=? AND deleted=0"},
            new String[]{"entity_event_types",   "UPDATE entity_event_types SET deleted=1, is_enabled=0 WHERE industry=? AND deleted=0"},
            new String[]{"trigger_points",       "UPDATE trigger_points SET deleted=1, is_enabled=0 WHERE industry=? AND deleted=0"},
            new String[]{"event_triggers",       "UPDATE event_triggers SET deleted=1, is_enabled=0 WHERE industry=? AND deleted=0"},
            new String[]{"relation_types",       "UPDATE relation_types SET is_enabled=0 WHERE industry=? AND is_enabled=1"},
            new String[]{"data_scope_dims",      "UPDATE data_scope_dims SET is_enabled=0 WHERE industry=? AND is_enabled=1"}
        );
        for (String[] t : tables) {
            int n = jdbc.update(t[1], code);
            cascadeCounts.put(t[0], n);
        }
        // 插件包本身
        jdbc.update("UPDATE plugin_packages SET enabled=0 WHERE industry_code=?", code);

        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "UNINSTALL:" + code));

        int total = cascadeCounts.values().stream().mapToInt(Integer::intValue).sum();
        return Result.success(Map.of(
            "code", code,
            "cascadeCounts", cascadeCounts,
            "totalAffected", total,
            "message", "插件 " + code + " 已卸载,软删 " + total + " 条声明 (CUSTOM 资源不受影响)"
        ));
    }

    // ═══════════════ Phase 5: 租户级插件管理 ═══════════════

    /**
     * GET /api/plugin-platform/tenants/{tenantId}/plugins
     * 列出某租户的所有插件及启用状态.
     */
    @GetMapping("/tenants/{tenantId}/plugins")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<List<Map<String, Object>>> tenantPlugins(@PathVariable Long tenantId) {
        return Result.success(tenantPluginService.allPluginsForTenant(tenantId));
    }

    /**
     * POST /api/plugin-platform/tenants/{tenantId}/plugins/{code}/enable
     */
    @PostMapping("/tenants/{tenantId}/plugins/{code}/enable")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> enableForTenant(
            @PathVariable Long tenantId, @PathVariable String code,
            @RequestBody(required = false) Map<String, Object> body) {
        String notes = body != null ? String.valueOf(body.getOrDefault("notes", "")) : "";
        tenantPluginService.enableForTenant(tenantId, code,
            com.school.management.common.util.SecurityUtils.getCurrentUserId(), notes);
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this,
            "TENANT_ENABLE:" + tenantId + ":" + code));
        return Result.success(Map.of("tenantId", tenantId, "code", code, "enabled", true));
    }

    /**
     * GET /api/plugin-platform/tenants/{tenantId}/plugins/{code}/config
     */
    @GetMapping("/tenants/{tenantId}/plugins/{code}/config")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> readTenantConfig(
            @PathVariable Long tenantId, @PathVariable String code) {
        return Result.success(tenantPluginService.readConfig(tenantId, code));
    }

    /**
     * PUT /api/plugin-platform/tenants/{tenantId}/plugins/{code}/config
     */
    @PutMapping("/tenants/{tenantId}/plugins/{code}/config")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> writeTenantConfig(
            @PathVariable Long tenantId, @PathVariable String code,
            @RequestBody Map<String, Object> config) {
        tenantPluginService.writeConfig(tenantId, code, config,
            com.school.management.common.util.SecurityUtils.getCurrentUserId());
        return Result.success(Map.of("tenantId", tenantId, "code", code, "config", config));
    }

    /**
     * POST /api/plugin-platform/tenants/{tenantId}/plugins/{code}/disable
     */
    @PostMapping("/tenants/{tenantId}/plugins/{code}/disable")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> disableForTenant(
            @PathVariable Long tenantId, @PathVariable String code,
            @RequestBody(required = false) Map<String, Object> body) {
        if ("CORE".equalsIgnoreCase(code)) {
            return Result.error("CORE 不可在租户级禁用");
        }
        String notes = body != null ? String.valueOf(body.getOrDefault("notes", "")) : "";
        tenantPluginService.disableForTenant(tenantId, code,
            com.school.management.common.util.SecurityUtils.getCurrentUserId(), notes);
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this,
            "TENANT_DISABLE:" + tenantId + ":" + code));
        return Result.success(Map.of("tenantId", tenantId, "code", code, "enabled", false));
    }

    /**
     * GET /api/plugin-platform/dependency-graph
     * 返回插件依赖图 — 节点/边数组 + GraphViz DOT 格式.
     * 前端可直接用 viz.js 渲染, 也可自行画布实现.
     */
    @GetMapping("/dependency-graph")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> dependencyGraph() {
        List<Map<String, Object>> pkgs = jdbc.queryForList(
            "SELECT industry_code, industry_name, version, enabled, depends_on " +
            "  FROM plugin_packages ORDER BY (industry_code='CORE') DESC, industry_code");

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        StringBuilder dot = new StringBuilder();
        dot.append("digraph PluginDeps {\n");
        dot.append("  rankdir=LR; node [shape=box, style=rounded];\n");

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        for (Map<String, Object> p : pkgs) {
            String code = (String) p.get("industry_code");
            String name = (String) p.get("industry_name");
            String ver = (String) p.get("version");
            int enabled = ((Number) p.getOrDefault("enabled", 0)).intValue();

            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", code);
            node.put("label", name + " v" + ver);
            node.put("enabled", enabled == 1);
            nodes.add(node);

            String color = enabled == 1 ? "#2563eb" : "#9ca3af";
            dot.append(String.format("  \"%s\" [label=\"%s\\nv%s\", color=\"%s\"];%n",
                code, name, ver, color));

            // 解析 depends_on JSON
            Object depsJson = p.get("depends_on");
            List<String> deps = List.of();
            if (depsJson != null && !depsJson.toString().isBlank()) {
                try {
                    deps = om.readValue(depsJson.toString(), List.class);
                } catch (Exception ignored) {}
            }
            for (String dep : deps) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("from", code);
                edge.put("to", dep);
                edges.add(edge);
                dot.append(String.format("  \"%s\" -> \"%s\";%n", code, dep));
            }
        }
        dot.append("}\n");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("dot", dot.toString());
        return Result.success(result);
    }

    /**
     * GET /api/plugin-platform/metrics
     * 返回所有 Registrar 的启动耗时 + 声明数 (Phase 9 metrics).
     */
    @GetMapping("/metrics")
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> metrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> registrars = new LinkedHashMap<>();
        for (var e : com.school.management.infrastructure.extension.AbstractPluginRegistrar.STARTUP_DURATION_MS.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("durationMs", e.getValue());
            r.put("declarationCount",
                com.school.management.infrastructure.extension.AbstractPluginRegistrar
                    .STARTUP_DECLARATION_COUNT.getOrDefault(e.getKey(), 0));
            registrars.put(e.getKey(), r);
        }
        result.put("registrars", registrars);
        result.put("totalDurationMs",
            com.school.management.infrastructure.extension.AbstractPluginRegistrar
                .STARTUP_DURATION_MS.values().stream().mapToLong(Long::longValue).sum());
        return Result.success(result);
    }
}
