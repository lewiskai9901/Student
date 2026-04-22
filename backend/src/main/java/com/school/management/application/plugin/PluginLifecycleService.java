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

    /** 10 张贡献表 + 其 industry 归属列 (此项目所有 10 表都叫 industry). */
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
        log.info("[PluginLifecycle] disabling {} -> cascade 9 tables", industryCode);

        // 2. 9 表级联 plugin_enabled=0 (不动 is_enabled, 保留管理员手动决定)
        for (String[] t : CONTRIBUTION_TABLES) {
            try {
                int rows = jdbc.update(
                    "UPDATE " + t[0] + " SET plugin_enabled=0 WHERE " + t[1] + "=?",
                    industryCode);
                log.info("  [{}] plugin_enabled=0 -> {} rows", t[0], rows);
            } catch (Exception e) {
                log.warn("  [{}] cascade failed: {}", t[0], e.getMessage());
            }
        }

        // 3. msg_subscription_rules 特殊: 按被禁插件的 event_type_code 反查
        try {
            int rows = jdbc.update(
                "UPDATE msg_subscription_rules r SET r.plugin_enabled=0 " +
                "WHERE r.event_type IN (SELECT type_code FROM entity_event_types WHERE industry=?)",
                industryCode);
            log.info("  [msg_subscription_rules by event_type] -> {} rows", rows);
        } catch (Exception e) {
            log.warn("subscription rules by event_type cascade failed: {}", e.getMessage());
        }

        // 4. 广播: Casbin 重载 / DataScope 缓存失效 / 菜单重构
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
        log.info("[PluginLifecycle] enabling {} -> cascade 9 tables", industryCode);

        for (String[] t : CONTRIBUTION_TABLES) {
            try {
                int rows = jdbc.update(
                    "UPDATE " + t[0] + " SET plugin_enabled=1 WHERE " + t[1] + "=?",
                    industryCode);
                log.info("  [{}] plugin_enabled=1 -> {} rows", t[0], rows);
            } catch (Exception e) {
                log.warn("  [{}] cascade failed: {}", t[0], e.getMessage());
            }
        }

        try {
            int rows = jdbc.update(
                "UPDATE msg_subscription_rules r SET r.plugin_enabled=1 " +
                "WHERE r.event_type IN (SELECT type_code FROM entity_event_types WHERE industry=?)",
                industryCode);
            log.info("  [msg_subscription_rules by event_type] -> {} rows", rows);
        } catch (Exception e) {
            log.warn("subscription rules by event_type cascade failed: {}", e.getMessage());
        }

        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "PLUGIN_ENABLE:" + industryCode));
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
