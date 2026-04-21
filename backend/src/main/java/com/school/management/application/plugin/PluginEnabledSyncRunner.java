package com.school.management.application.plugin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时按 plugin_packages.enabled 同步所有贡献表的 plugin_enabled 列.
 *
 * 背景: 各 Registrar (AbstractPluginRegistrar 子类) 启动时 UPSERT 业务字段, 不碰
 * plugin_enabled. 若插件之前被禁 (enabled=0), UPSERT 新增/更新的行默认 plugin_enabled=1,
 * 会导致 disable 效果被 restart 后部分覆盖. 本 Runner 在 ApplicationReady 之后跑一次,
 * 把所有 enabled=0 的插件的贡献全部再置为 plugin_enabled=0, 保证 restart 后状态一致.
 *
 * Order 较晚, 跑在所有 Registrar 之后.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginEnabledSyncRunner {

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1000)
    public void syncOnStartup() {
        List<String> disabled;
        try {
            disabled = jdbc.queryForList(
                "SELECT industry_code FROM plugin_packages WHERE enabled=0", String.class);
        } catch (Exception e) {
            log.warn("[PluginEnabledSync] plugin_packages 表未就绪, 跳过同步: {}", e.getMessage());
            return;
        }
        if (disabled.isEmpty()) {
            log.info("[PluginEnabledSync] 无禁用插件, 跳过");
            return;
        }
        log.info("[PluginEnabledSync] 同步 {} 个禁用插件状态: {}", disabled.size(), disabled);

        int totalRows = 0;
        for (String code : disabled) {
            for (String[] t : PluginLifecycleService.CONTRIBUTION_TABLES) {
                try {
                    int n = jdbc.update(
                        "UPDATE " + t[0] + " SET plugin_enabled=0 " +
                        "WHERE " + t[1] + "=? AND plugin_enabled<>0",
                        code);
                    if (n > 0) {
                        totalRows += n;
                        log.info("  [{}] {} -> plugin_enabled=0 ({} rows)", t[0], code, n);
                    }
                } catch (Exception e) {
                    // 列不存在/老环境 — 跳过
                }
            }
            // msg_subscription_rules 特殊
            try {
                int n = jdbc.update(
                    "UPDATE msg_subscription_rules r SET r.plugin_enabled=0 " +
                    "WHERE r.event_type IN (SELECT type_code FROM entity_event_types WHERE industry=?) " +
                    "  AND r.plugin_enabled<>0",
                    code);
                if (n > 0) {
                    totalRows += n;
                    log.info("  [msg_subscription_rules by event_type] {} -> {} rows", code, n);
                }
            } catch (Exception ignored) {}
        }
        log.info("[PluginEnabledSync] 完成, 共同步 {} 行", totalRows);
    }
}
