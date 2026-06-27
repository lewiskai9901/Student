package com.school.management.application.plugin;

import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 插件生命周期管理服务 — enable/disable 时级联软失效/恢复 9 张贡献表的 plugin_enabled 列.
 *
 * 两状态模型:
 *   - is_enabled     : 管理员手动开关 (单条, 保留不动)
 *   - plugin_enabled : 所属插件级开关 (批量, 由本 service 管)
 * 实际生效 = is_enabled AND plugin_enabled
 *
 * 依赖检查: 禁用插件前检查是否有其他已启用插件依赖它; 有则抛 IllegalStateException.
 *
 * 事件广播: 完成后发 {@link PermissionsRefreshedEvent} 让 Casbin / PluginDataScopeRouter
 * 清缓存 + 重载策略.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginLifecycleService {

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 贡献表清单 (单一真相; disable/enable/uninstall 共用) + 其 industry 归属列。
     * 这些表都有 plugin_enabled 列, 走两态模型 (effective = is_enabled AND plugin_enabled)。
     * ⚠ 不含 resource_relations: 该表无 plugin_enabled 列 (其 reader ResourceRelationRegistry 仅按
     *   enabled=1 过滤, 且 ResourceRelationUpserter 启动期 enabled=1 复活) → 暂不纳入两态级联;
     *   要纳入需另加列 + 改 registry 过滤 + upserter 保留, 属独立改动 (P0 暂不做, 见审计 H2 备注)。
     */
    public static final List<String[]> CONTRIBUTION_TABLES = List.of(
        new String[]{"entity_type_configs",  "industry"},
        new String[]{"relation_types",       "industry"},
        new String[]{"entity_event_types",   "industry"},
        new String[]{"trigger_points",       "industry"},
        new String[]{"event_triggers",       "industry"},
        new String[]{"roles",                "industry"},
        new String[]{"permissions",          "industry"},
        new String[]{"data_scope_dims",      "industry"},
        new String[]{"data_resources",       "industry"}
        // msg_subscription_rules 无 industry 列, 单独按 event_type 级联 (见下)
    );

    /**
     * 禁用插件 + 级联软失效所有贡献.
     * @throws IllegalStateException 若有其他启用插件依赖本插件或 CORE 尝试被禁.
     */
    @Transactional
    public void disable(String industryCode) {
        if ("CORE".equalsIgnoreCase(industryCode)) {
            throw new IllegalStateException("CORE 包不可禁用");
        }
        assertNotDependedOn(industryCode);

        // 1. 主表: 设 enabled=0 + last_disabled_at
        int n = jdbc.update(
            "UPDATE plugin_packages SET enabled=0, last_disabled_at=NOW() WHERE industry_code=?",
            industryCode);
        if (n == 0) {
            throw new IllegalStateException("插件不存在: " + industryCode);
        }
        // 2. 级联 plugin_enabled=0 (单一 cascade 助手; disable/enable/uninstall 共用同一张表清单)
        cascadePluginEnabled(industryCode, 0);

        // 3. 广播: Casbin 重载 / DataScope 缓存失效 / 菜单重构
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "PLUGIN_DISABLE:" + industryCode));
    }

    /**
     * 启用插件 + 级联恢复 plugin_enabled=1 (管理员手动 is_enabled=0 保留不变).
     */
    @Transactional
    public void enable(String industryCode) {
        int n = jdbc.update(
            "UPDATE plugin_packages SET enabled=1, last_started_at=NOW() WHERE industry_code=?",
            industryCode);
        if (n == 0) {
            throw new IllegalStateException("插件不存在: " + industryCode);
        }
        cascadePluginEnabled(industryCode, 1);
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "PLUGIN_ENABLE:" + industryCode));
    }

    /**
     * 卸载插件 (SOFT) —— 与 disable 同一持久机制 (plugin_enabled=0), 收敛掉旧的
     * {@code PluginPlatformApplicationService.uninstallPlugin} (它用 deleted=1/is_enabled=0, 而这些列
     * 被各 Registrar 启动期无条件重置 → uninstall 对编译进单体的插件重启即复活; 且漏清 data_resources/
     * resource_relations。修复见 2026-06-27 P0-H2)。
     *
     * <p><b>语义说明</b>: 单体内插件代码编译在仓内, 无法真正"删除"(重启 contribution 会重新 upsert 行);
     * 故 SOFT uninstall = 持久禁用 (plugin_enabled=0 是唯一不被启动期重置的列)。功能等同 disable,
     * 仅事件原因与返回(各表级联行数)不同, 供平台展示。
     *
     * @return 各贡献表级联行数 (供 UI 显示)
     */
    @Transactional
    public java.util.Map<String, Integer> uninstall(String industryCode) {
        if ("CORE".equalsIgnoreCase(industryCode)) {
            throw new IllegalStateException("CORE 包不可卸载");
        }
        assertNotDependedOn(industryCode);
        int n = jdbc.update(
            "UPDATE plugin_packages SET enabled=0, last_disabled_at=NOW() WHERE industry_code=?",
            industryCode);
        if (n == 0) {
            throw new IllegalStateException("插件不存在: " + industryCode);
        }
        log.info("[PluginLifecycle] uninstalling(SOFT) {} -> cascade {} tables", industryCode, CONTRIBUTION_TABLES.size());
        java.util.Map<String, Integer> counts = cascadePluginEnabled(industryCode, 0);
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "PLUGIN_UNINSTALL:" + industryCode));
        return counts;
    }

    /**
     * 级联 plugin_enabled (disable=0 / enable=1 / uninstall=0) —— 单一真相: 同一张贡献表清单 +
     * msg_subscription_rules 特例。不动 is_enabled (管理员手动开关) / deleted (各 Registrar 启动期重置)。
     * 返回各表受影响行数。
     */
    private java.util.Map<String, Integer> cascadePluginEnabled(String industryCode, int value) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String[] t : CONTRIBUTION_TABLES) {
            try {
                int rows = jdbc.update(
                    "UPDATE " + t[0] + " SET plugin_enabled=? WHERE " + t[1] + "=?",
                    value, industryCode);
                counts.put(t[0], rows);
                log.info("  [{}] plugin_enabled={} -> {} rows", t[0], value, rows);
            } catch (Exception e) {
                counts.put(t[0], -1);
                log.warn("  [{}] cascade failed: {}", t[0], e.getMessage());
            }
        }
        // msg_subscription_rules 无 industry 列, 按 event_type 反查级联
        try {
            int rows = jdbc.update(
                "UPDATE msg_subscription_rules r SET r.plugin_enabled=? " +
                "WHERE r.event_type IN (SELECT type_code FROM entity_event_types WHERE industry=?)",
                value, industryCode);
            counts.put("msg_subscription_rules", rows);
            log.info("  [msg_subscription_rules by event_type] -> {} rows", rows);
        } catch (Exception e) {
            counts.put("msg_subscription_rules", -1);
            log.warn("subscription rules by event_type cascade failed: {}", e.getMessage());
        }
        return counts;
    }

    /** 依赖检查: 其他 enabled 插件的 depends_on JSON 是否包含本 code. */
    private void assertNotDependedOn(String industryCode) {
        try {
            List<String> dependents = jdbc.queryForList(
                "SELECT industry_code FROM plugin_packages " +
                "WHERE enabled=1 AND industry_code <> ? " +
                "  AND JSON_CONTAINS(depends_on, JSON_QUOTE(?))",
                String.class, industryCode, industryCode);
            if (!dependents.isEmpty()) {
                throw new IllegalStateException(
                    "无法禁用 " + industryCode + ": 被以下启用插件依赖 " + dependents
                        + ". 请先禁用它们.");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 旧环境无 depends_on 列时放过
            log.warn("dependency check skipped: {}", e.getMessage());
        }
    }
}
