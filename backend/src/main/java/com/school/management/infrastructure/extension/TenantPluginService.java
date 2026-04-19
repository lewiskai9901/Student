package com.school.management.infrastructure.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户级插件启用服务 (Phase 5).
 *
 * 查询"当前 tenant 是否启用插件 X" 或 "当前 tenant 启用了哪些插件" 的权威入口.
 *
 * 判定规则:
 *   plugin_packages.enabled = 1     (全局已装)
 *   AND tenant_plugin_enablement.enabled = 1  (租户启用)
 *
 * 若 tenant_plugin_enablement 无该行, 视为未启用 (严格模式).
 *
 * 查询结果带 30 秒内存 cache, 避免每次 DB 打点.
 * admin 修改启用状态时应调用 {@link #invalidate(Long)} 清缓存.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPluginService {

    private final JdbcTemplate jdbc;

    /** 每租户的 enabled plugins 缓存: tenantId -> Set<pluginCode> */
    private final Map<Long, CachedEntry> cache = new ConcurrentHashMap<>();

    private static final long CACHE_TTL_MS = 30_000L;

    /** 指定租户启用的所有插件码 */
    public Set<String> enabledPlugins(Long tenantId) {
        if (tenantId == null) return Set.of();
        CachedEntry e = cache.get(tenantId);
        long now = System.currentTimeMillis();
        if (e != null && now - e.loadedAt < CACHE_TTL_MS) return e.codes;

        Set<String> codes = new HashSet<>(jdbc.queryForList(
            "SELECT pkg.industry_code FROM plugin_packages pkg " +
            "INNER JOIN tenant_plugin_enablement tpe " +
            "        ON tpe.plugin_code = pkg.industry_code COLLATE utf8mb4_unicode_ci " +
            "       AND tpe.tenant_id = ? " +
            "       AND tpe.enabled = 1 " +
            "WHERE pkg.enabled = 1",
            String.class, tenantId));
        cache.put(tenantId, new CachedEntry(codes, now));
        return codes;
    }

    /** 指定租户是否启用了某个插件 */
    public boolean isEnabled(Long tenantId, String pluginCode) {
        return enabledPlugins(tenantId).contains(pluginCode);
    }

    /** admin 变更后清缓存 */
    public void invalidate(Long tenantId) {
        if (tenantId == null) cache.clear();
        else cache.remove(tenantId);
    }

    /**
     * 读取某租户某插件的配置 JSON (返回 Map, 若无配置返回空 Map).
     */
    public Map<String, Object> readConfig(Long tenantId, String pluginCode) {
        try {
            String json = jdbc.query(
                "SELECT config_json FROM tenant_plugin_enablement " +
                "WHERE tenant_id=? AND plugin_code=?",
                rs -> rs.next() ? rs.getString(1) : null,
                tenantId, pluginCode);
            if (json == null || json.isBlank()) return Map.of();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            return om.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("[TenantPlugin] 读取 config_json 失败 tenant={} plugin={}: {}",
                tenantId, pluginCode, e.getMessage());
            return Map.of();
        }
    }

    /**
     * 写入某租户某插件的配置 JSON (replace 策略).
     */
    public void writeConfig(Long tenantId, String pluginCode, Map<String, Object> config, Long operatorId) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = om.writeValueAsString(config == null ? Map.of() : config);
            int n = jdbc.update(
                "UPDATE tenant_plugin_enablement SET config_json=?, enabled_by=?, enabled_at=NOW() " +
                "WHERE tenant_id=? AND plugin_code=?",
                json, operatorId, tenantId, pluginCode);
            if (n == 0) {
                // 行不存在, 自动创建 (默认 enabled=1)
                jdbc.update(
                    "INSERT INTO tenant_plugin_enablement (tenant_id, plugin_code, enabled, config_json, enabled_by, enabled_at, notes) " +
                    "VALUES (?, ?, 1, ?, ?, NOW(), 'auto-created by config write')",
                    tenantId, pluginCode, json, operatorId);
            }
            invalidate(tenantId);
            log.info("[TenantPlugin] 租户 {} 插件 {} 配置更新 by user={}", tenantId, pluginCode, operatorId);
        } catch (Exception e) {
            throw new RuntimeException("写入插件 config_json 失败: " + e.getMessage(), e);
        }
    }

    /** 启用某租户的某插件 */
    public void enableForTenant(Long tenantId, String pluginCode, Long operatorId, String notes) {
        jdbc.update(
            "INSERT INTO tenant_plugin_enablement (tenant_id, plugin_code, enabled, enabled_by, enabled_at, notes) " +
            "VALUES (?, ?, 1, ?, NOW(), ?) " +
            "ON DUPLICATE KEY UPDATE enabled=1, enabled_by=?, enabled_at=NOW(), notes=?",
            tenantId, pluginCode, operatorId, notes, operatorId, notes);
        invalidate(tenantId);
        log.info("[TenantPlugin] 租户 {} 启用插件 {} by user={}", tenantId, pluginCode, operatorId);
    }

    /** 禁用某租户的某插件 */
    public void disableForTenant(Long tenantId, String pluginCode, Long operatorId, String notes) {
        jdbc.update(
            "INSERT INTO tenant_plugin_enablement (tenant_id, plugin_code, enabled, enabled_by, enabled_at, notes) " +
            "VALUES (?, ?, 0, ?, NOW(), ?) " +
            "ON DUPLICATE KEY UPDATE enabled=0, enabled_by=?, enabled_at=NOW(), notes=?",
            tenantId, pluginCode, operatorId, notes, operatorId, notes);
        invalidate(tenantId);
        log.info("[TenantPlugin] 租户 {} 禁用插件 {} by user={}", tenantId, pluginCode, operatorId);
    }

    /** 查询某租户所有插件状态 (启用+未启用都返回) */
    public List<Map<String, Object>> allPluginsForTenant(Long tenantId) {
        return jdbc.queryForList(
            "SELECT pkg.industry_code AS code, pkg.industry_name AS name, pkg.version, " +
            "       pkg.enabled AS globalEnabled, " +
            "       COALESCE(tpe.enabled, 0) AS tenantEnabled, " +
            "       tpe.enabled_at AS enabledAt, tpe.notes " +
            "  FROM plugin_packages pkg " +
            "  LEFT JOIN tenant_plugin_enablement tpe " +
            "    ON tpe.plugin_code = pkg.industry_code COLLATE utf8mb4_unicode_ci " +
            "   AND tpe.tenant_id = ? " +
            " ORDER BY (pkg.industry_code='CORE') DESC, pkg.industry_code",
            tenantId);
    }

    private record CachedEntry(Set<String> codes, long loadedAt) {}
}
