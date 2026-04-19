package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息域 Registrar — 聚合 3 个子 Registrar 的统一入口, 对外只暴露一条日志.
 *
 * 内部分工(使用 {@link AbstractPluginRegistrar} 基类):
 *   - {@link TriggerPointSubRegistrar}    写 trigger_points
 *   - {@link EventTypeSubRegistrar}        写 entity_event_types
 *   - {@link DefaultTriggerSubRegistrar}  写 event_triggers (仅 INSERT, 不覆盖 admin 修改)
 *
 * @Order(300): EntityType(100) + Relation(200) 之后.
 */
@Slf4j
@Component
@Order(300)
@RequiredArgsConstructor
public class MessagingRegistrar implements ApplicationRunner {

    private final List<MessagingDomainPlugin> plugins;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final PluginPackageRegistrar packageRegistrar;

    @Override
    public void run(ApplicationArguments args) {
        if (plugins.isEmpty()) {
            log.info("[MessagingRegistrar] 无消息插件");
            return;
        }

        int tp = 0, et = 0, dt = 0;

        // 1. 触发点
        for (MessagingDomainPlugin p : plugins) {
            String industry = packageRegistrar.resolveIndustry(p.getClass());
            String pluginClass = p.getClass().getName();
            for (var def : p.triggerPoints()) {
                try { upsertTriggerPoint(p, def, industry, pluginClass); tp++; }
                catch (Exception e) { log.error("[MessagingRegistrar] 触发点写入失败 {}: {}", def.pointCode(), e.getMessage()); }
            }
        }

        // 2. 事件类型
        for (MessagingDomainPlugin p : plugins) {
            String industry = packageRegistrar.resolveIndustry(p.getClass());
            String pluginClass = p.getClass().getName();
            for (var def : p.eventTypes()) {
                try { upsertEventType(p, def, industry, pluginClass); et++; }
                catch (Exception e) { log.error("[MessagingRegistrar] 事件类型写入失败 {}: {}", def.typeCode(), e.getMessage()); }
            }
        }

        // 3. 默认触发器
        for (MessagingDomainPlugin p : plugins) {
            String pluginClass = p.getClass().getName();
            for (var def : p.defaultTriggers()) {
                try { upsertDefaultTrigger(p, def, pluginClass); dt++; }
                catch (Exception e) { log.error("[MessagingRegistrar] 默认触发器写入失败 {}->{}: {}",
                        def.pointCode(), def.eventTypeCode(), e.getMessage()); }
            }
        }

        log.info("[MessagingRegistrar] 扫描 {} 个域插件: 触发点 {} / 事件类型 {} / 默认触发器 {}",
            plugins.size(), tp, et, dt);
    }

    // ═══════════════ 3 个写入子方法 ═══════════════
    // 注: 使用 AbstractPluginRegistrar 把这 3 个拆成 3 个 Sub-Registrar 也可行,
    //     但需要协调 @Order 子序, 且总日志拆成 3 条. 此处用组合聚合更清晰.

    private void upsertTriggerPoint(MessagingDomainPlugin plugin,
                                     MessagingDomainPlugin.TriggerPointDef def,
                                     String industry, String pluginClass) throws Exception {
        String existingIndustry = jdbc.query(
            "SELECT industry FROM trigger_points WHERE point_code=? AND tenant_id=1 AND deleted=0",
            rs -> rs.next() ? rs.getString(1) : null, def.pointCode());
        if ("CUSTOM".equals(existingIndustry)) return;

        String origin = packageRegistrar.resolveOrigin(plugin.getClass());
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM trigger_points WHERE point_code=? AND tenant_id=1 AND deleted=0",
            Long.class, def.pointCode());

        String contextSchemaJson = def.contextSchema() != null
            ? objectMapper.writeValueAsString(def.contextSchema())
            : null;

        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO trigger_points (module_code, module_name, point_code, point_name, " +
                "description, context_schema, is_enabled, tenant_id, industry, plugin_class, origin, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?, NOW())",
                plugin.getDomainCode(), plugin.getDomainName(),
                def.pointCode(), def.pointName(), def.description(), contextSchemaJson,
                industry, pluginClass, origin);
        } else {
            jdbc.update(
                "UPDATE trigger_points SET module_code=?, module_name=?, point_name=?, " +
                "description=?, context_schema=?, industry=?, plugin_class=?, origin=?, is_enabled=1 " +
                "WHERE point_code=? AND tenant_id=1 AND deleted=0",
                plugin.getDomainCode(), plugin.getDomainName(), def.pointName(),
                def.description(), contextSchemaJson, industry, pluginClass, origin, def.pointCode());
        }
    }

    private void upsertEventType(MessagingDomainPlugin plugin,
                                  MessagingDomainPlugin.EventTypeDef def,
                                  String industry, String pluginClass) {
        String existingIndustry = jdbc.query(
            "SELECT industry FROM entity_event_types WHERE type_code=? AND tenant_id=1 AND deleted=0",
            rs -> rs.next() ? rs.getString(1) : null, def.typeCode());
        if ("CUSTOM".equals(existingIndustry)) return;

        String origin = packageRegistrar.resolveOrigin(plugin.getClass());
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM entity_event_types WHERE type_code=? AND tenant_id=1 AND deleted=0",
            Long.class, def.typeCode());

        String applicableSubjects = def.applicableSubjects() != null
            ? String.join(",", def.applicableSubjects())
            : null;

        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO entity_event_types (tenant_id, category_code, category_name, " +
                "type_code, type_name, category_polarity, icon, color, applicable_subjects, " +
                "is_system, is_enabled, industry, plugin_class, origin, created_at) " +
                "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?, NOW())",
                def.categoryCode(), def.categoryName(),
                def.typeCode(), def.typeName(), def.categoryPolarity(),
                def.icon(), def.color(), applicableSubjects, industry, pluginClass, origin);
        } else {
            jdbc.update(
                "UPDATE entity_event_types SET category_code=?, category_name=?, type_name=?, " +
                "category_polarity=?, icon=?, color=?, applicable_subjects=?, " +
                "is_system=1, is_enabled=1, industry=?, plugin_class=?, origin=? " +
                "WHERE type_code=? AND tenant_id=1 AND deleted=0",
                def.categoryCode(), def.categoryName(), def.typeName(),
                def.categoryPolarity(), def.icon(), def.color(), applicableSubjects,
                industry, pluginClass, origin, def.typeCode());
        }
    }

    private void upsertDefaultTrigger(MessagingDomainPlugin plugin,
                                       MessagingDomainPlugin.DefaultTriggerDef def,
                                       String pluginClass) throws Exception {
        String origin = packageRegistrar.resolveOrigin(plugin.getClass());
        String industry = packageRegistrar.resolveIndustry(plugin.getClass());

        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM event_triggers " +
            "WHERE trigger_point_code=? AND event_type_code=? AND deleted=0",
            Long.class, def.pointCode(), def.eventTypeCode());

        if (exists != null && exists > 0) {
            // 已存在 — 不覆盖 admin 调整的 name/description, 但回填 origin/industry/plugin_class
            jdbc.update(
                "UPDATE event_triggers SET industry=?, plugin_class=?, origin=? " +
                "WHERE trigger_point_code=? AND event_type_code=? AND deleted=0 " +
                "AND (industry IS NULL OR origin IS NULL)",
                industry, pluginClass, origin, def.pointCode(), def.eventTypeCode());
            return;
        }

        String name = "默认: " + def.pointCode() + " → " + def.eventTypeCode();
        String description = "由插件 " + pluginClass + " 注册的默认触发器";
        jdbc.update(
            "INSERT INTO event_triggers (tenant_id, name, trigger_point_code, event_type_mode, " +
            "event_type_code, description, is_enabled, industry, plugin_class, origin, created_at) " +
            "VALUES (1, ?, ?, 'FIXED', ?, ?, 1, ?, ?, ?, NOW())",
            name, def.pointCode(), def.eventTypeCode(), description,
            industry, pluginClass, origin);
    }
}
