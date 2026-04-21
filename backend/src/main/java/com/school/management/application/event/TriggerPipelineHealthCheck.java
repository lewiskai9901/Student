package com.school.management.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动时检查事件/消息配置表完整性.
 *
 * 4 张表任意为空都会让 triggerService.fire() 静默 no-op:
 *   - trigger_points          (插件声明的触发点码)
 *   - entity_event_types      (事件类型目录)
 *   - event_triggers          (触发器配置)
 *   - msg_subscription_rules  (订阅规则)
 *
 * ApplicationReadyEvent 启动末尾跑一遍, 缺就 ERROR 日志 + isHealthy()=false.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerPipelineHealthCheck {

    private final JdbcTemplate jdbc;
    private volatile boolean healthy = false;
    private volatile List<String> missingTables = List.of();

    public boolean isHealthy() { return healthy; }
    public List<String> getMissingTables() { return missingTables; }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        // LinkedHashMap 保持顺序, 报告更可读
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("trigger_points",         "SELECT COUNT(*) FROM trigger_points WHERE is_enabled=1");
        checks.put("entity_event_types",     "SELECT COUNT(*) FROM entity_event_types WHERE is_enabled=1 AND deleted=0");
        checks.put("event_triggers",         "SELECT COUNT(*) FROM event_triggers WHERE is_enabled=1 AND deleted=0");
        checks.put("msg_subscription_rules", "SELECT COUNT(*) FROM msg_subscription_rules WHERE is_enabled=1");

        List<String> missing = new ArrayList<>();
        for (var e : checks.entrySet()) {
            try {
                Long n = jdbc.queryForObject(e.getValue(), Long.class);
                if (n == null || n == 0) {
                    missing.add(e.getKey() + " (count=0)");
                }
            } catch (Exception ex) {
                missing.add(e.getKey() + " (query failed: " + ex.getMessage() + ")");
            }
        }

        this.missingTables = List.copyOf(missing);
        this.healthy = missing.isEmpty();

        if (!healthy) {
            log.error("[TriggerPipelineHealthCheck] 消息/事件管道不完整, 以下表空或缺失: {} — " +
                     "triggerService.fire() 将静默 no-op, 业务消息推送全盲! " +
                     "检查 migration (V97/V98/...) 是否 apply", missingTables);
        } else {
            log.info("[TriggerPipelineHealthCheck] 消息/事件管道就绪");
        }
    }
}
