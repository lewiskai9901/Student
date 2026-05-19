package com.school.management.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.util.PluginEnabledGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.school.management.common.util.SnakeToCamelUtil.toCamelCase;
import static com.school.management.common.util.SnakeToCamelUtil.toCamelCaseList;

/**
 * 事件配置应用服务 (L3, 2026-05-19).
 *
 * <p>L3 收拢前 interfaces/rest/event/* 三 Controller (EventTriggerController /
 * EventTypeController / TriggerPointController) 31 处直 jdbc CRUD 到此服务.
 *
 * <p>这是配置元数据 (CRUD over 3 张配置表), 没有聚合根值得抽象 —
 * 走 jdbc + Map 是合理的, 但**必须发生在 application 层不是 interfaces 层**,
 * 这样 ArchUnit 守护可以禁住 interfaces.rest 直访 jdbc.
 *
 * <p>顺手修了 TriggerPointController.list() 的 SQL 注入 (单引号 replace 防御不够,
 * 用参数化).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConfigApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PluginEnabledGuard pluginEnabledGuard;
    private final TriggerService triggerService;

    // ==================== event_triggers ====================

    public List<Map<String, Object>> listTriggers(String pointCode, String eventType) {
        StringBuilder sql = new StringBuilder(
            "SELECT t.*, tp.point_name AS trigger_point_name, tp.module_code, tp.module_name " +
            "FROM event_triggers t " +
            "LEFT JOIN trigger_points tp ON t.trigger_point_code = tp.point_code AND tp.deleted = 0 " +
            "WHERE t.deleted = 0 AND t.plugin_enabled = 1");
        List<Object> params = new ArrayList<>();
        if (pointCode != null && !pointCode.isBlank()) {
            sql.append(" AND t.trigger_point_code = ?");
            params.add(pointCode);
        }
        if (eventType != null && !eventType.isBlank()) {
            sql.append(" AND t.event_type_code = ?");
            params.add(eventType);
        }
        sql.append(" ORDER BY t.sort_order, t.id");
        return toCamelCaseList(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    public Map<String, Object> getTriggerById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT t.*, tp.point_name AS trigger_point_name, tp.module_code, tp.module_name " +
            "FROM event_triggers t " +
            "LEFT JOIN trigger_points tp ON t.trigger_point_code = tp.point_code AND tp.deleted = 0 " +
            "WHERE t.id = ? AND t.deleted = 0", id);
        return rows.isEmpty() ? null : toCamelCase(rows.get(0));
    }

    @Transactional
    public void createTrigger(Map<String, Object> body) {
        String conditionJson = toJsonString(body.get("conditionJson"));
        String subjectsJson = toJsonString(body.get("subjectsJson"));
        jdbcTemplate.update(
            "INSERT INTO event_triggers (name, trigger_point_code, condition_json, " +
            "event_type_mode, event_type_code, event_type_source, " +
            "subjects_json, description, is_enabled, sort_order, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
            body.get("name"), body.get("triggerPointCode"), conditionJson,
            body.getOrDefault("eventTypeMode", "FIXED"),
            body.get("eventTypeCode"), body.get("eventTypeSource"),
            subjectsJson, body.get("description"),
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0));
    }

    @Transactional
    public void updateTrigger(Long id, Map<String, Object> body) {
        pluginEnabledGuard.check("event_triggers", id);
        String conditionJson = toJsonString(body.get("conditionJson"));
        String subjectsJson = toJsonString(body.get("subjectsJson"));
        jdbcTemplate.update(
            "UPDATE event_triggers SET name = ?, trigger_point_code = ?, condition_json = ?, " +
            "event_type_mode = ?, event_type_code = ?, event_type_source = ?, " +
            "subjects_json = ?, description = ?, sort_order = ? " +
            "WHERE id = ? AND deleted = 0",
            body.get("name"), body.get("triggerPointCode"), conditionJson,
            body.getOrDefault("eventTypeMode", "FIXED"),
            body.get("eventTypeCode"), body.get("eventTypeSource"),
            subjectsJson, body.get("description"),
            body.getOrDefault("sortOrder", 0),
            id);
    }

    @Transactional
    public void deleteTrigger(Long id) {
        pluginEnabledGuard.check("event_triggers", id);
        jdbcTemplate.update("UPDATE event_triggers SET deleted = 1 WHERE id = ?", id);
    }

    @Transactional
    public void enableTrigger(Long id) {
        pluginEnabledGuard.check("event_triggers", id);
        jdbcTemplate.update("UPDATE event_triggers SET is_enabled = 1 WHERE id = ? AND deleted = 0", id);
    }

    @Transactional
    public void disableTrigger(Long id) {
        pluginEnabledGuard.check("event_triggers", id);
        jdbcTemplate.update("UPDATE event_triggers SET is_enabled = 0 WHERE id = ? AND deleted = 0", id);
    }

    public List<Map<String, Object>> testTrigger(String pointCode, Map<String, Object> context) {
        return toCamelCaseList(triggerService.testFire(pointCode, context));
    }

    // ==================== entity_event_types ====================

    public List<Map<String, Object>> listEventTypesGrouped(String category, Boolean includeDisabled) {
        boolean admin = Boolean.TRUE.equals(includeDisabled);
        String sql = "SELECT * FROM entity_event_types WHERE deleted = 0";
        if (!admin) {
            sql += " AND plugin_enabled = 1";
        }
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isBlank()) {
            sql += " AND category_code = ?";
            params.add(category);
        }
        sql += " ORDER BY sort_order";
        List<Map<String, Object>> allTypes = jdbcTemplate.queryForList(sql, params.toArray());

        Map<String, List<Map<String, Object>>> grouped = allTypes.stream()
            .collect(Collectors.groupingBy(
                row -> String.valueOf(row.get("category_code")),
                LinkedHashMap::new,
                Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> types = entry.getValue();
            Map<String, Object> first = types.get(0);
            Map<String, Object> group = new LinkedHashMap<>();
            group.put("categoryCode", entry.getKey());
            group.put("categoryName", first.get("category_name"));
            group.put("categoryPolarity", first.get("category_polarity"));
            group.put("types", toCamelCaseList(types));
            result.add(group);
        }
        return result;
    }

    public List<Map<String, Object>> listEventCategories() {
        // 注: 原 Controller 有一段 dead-code (DISTINCT + ORDER BY MIN 无法工作),
        // 仅留可工作的 GROUP BY 分支.
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "SELECT category_code, category_name, category_polarity, " +
            "COUNT(*) as type_count, MIN(sort_order) as min_sort " +
            "FROM entity_event_types WHERE deleted = 0 " +
            "GROUP BY category_code, category_name, category_polarity " +
            "ORDER BY min_sort");
        return toCamelCaseList(result);
    }

    @Transactional
    public void createEventType(Map<String, Object> body) {
        String categoryPolarity = (String) body.getOrDefault("categoryPolarity", "NEUTRAL");
        if (body.get("categoryCode") != null) {
            try {
                String existingPolarity = jdbcTemplate.queryForObject(
                    "SELECT category_polarity FROM entity_event_types WHERE category_code = ? AND deleted = 0 LIMIT 1",
                    String.class, body.get("categoryCode"));
                if (existingPolarity != null) {
                    categoryPolarity = existingPolarity;
                }
            } catch (Exception ignored) {
                // 同分类首次创建 — fall through, 用入参 polarity
            }
        }
        jdbcTemplate.update(
            "INSERT INTO entity_event_types (tenant_id, category_code, category_name, category_polarity, " +
            "type_code, type_name, icon, color, applicable_subjects, " +
            "is_system, is_enabled, sort_order, industry) " +
            "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CUSTOM')",
            body.get("categoryCode"), body.get("categoryName"), categoryPolarity,
            body.get("typeCode"), body.get("typeName"),
            body.get("icon"), body.get("color"),
            body.get("applicableSubjects"),
            body.getOrDefault("isSystem", 0),
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0));
    }

    /**
     * @return true 已更新, false 实体不存在
     */
    @Transactional
    public boolean updateEventType(Long id, Map<String, Object> body) {
        pluginEnabledGuard.check("entity_event_types", id);
        // 系统预置类型: 只允许改分类归属与排序/启用状态, 核心定义字段强制保留, 防 UI 误改.
        // 分类级元数据 (categoryCode/Name/Polarity) 仍可改, 支持"分类重命名/合并".
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT is_system, type_name, icon, color, applicable_subjects " +
            "FROM entity_event_types WHERE id = ? AND deleted = 0", id);
        if (rows.isEmpty()) return false;
        Map<String, Object> row = rows.get(0);
        Object isSystem = row.get("is_system");
        boolean locked = isSystem != null && (Integer.valueOf(1).equals(isSystem) || Boolean.TRUE.equals(isSystem));

        Object typeName = locked ? row.get("type_name") : body.get("typeName");
        Object icon = locked ? row.get("icon") : body.get("icon");
        Object color = locked ? row.get("color") : body.get("color");
        Object applicableSubjects = locked ? row.get("applicable_subjects") : body.get("applicableSubjects");

        jdbcTemplate.update(
            "UPDATE entity_event_types SET category_code = ?, category_name = ?, category_polarity = ?, " +
            "type_name = ?, icon = ?, color = ?, applicable_subjects = ?, " +
            "is_enabled = ?, sort_order = ? " +
            "WHERE id = ? AND deleted = 0",
            body.get("categoryCode"), body.get("categoryName"),
            body.getOrDefault("categoryPolarity", "NEUTRAL"),
            typeName, icon, color, applicableSubjects,
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0),
            id);
        return true;
    }

    /**
     * @return null = 成功, 其他 = 错误信息 (系统预置不允许删除).
     */
    @Transactional
    public String deleteEventType(Long id) {
        pluginEnabledGuard.check("entity_event_types", id);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT is_system FROM entity_event_types WHERE id = ? AND deleted = 0", id);
        if (!rows.isEmpty()) {
            Object isSystem = rows.get(0).get("is_system");
            if (isSystem != null && (Integer.valueOf(1).equals(isSystem) || Boolean.TRUE.equals(isSystem))) {
                return "系统预置类型不允许删除";
            }
        }
        jdbcTemplate.update("UPDATE entity_event_types SET deleted = 1 WHERE id = ?", id);
        return null;
    }

    /**
     * 创建事件分类 (插入 placeholder type 让分类出现在查询).
     * Result: null=成功, 其他=错误信息.
     */
    @Transactional
    public String createEventCategory(String categoryCode, String categoryName, String polarity) {
        if (categoryCode == null || categoryName == null) {
            return "categoryCode和categoryName不能为空";
        }
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM entity_event_types WHERE category_code = ? AND deleted = 0",
            Integer.class, categoryCode);
        if (count != null && count > 0) {
            return "分类编码已存在: " + categoryCode;
        }
        jdbcTemplate.update(
            "INSERT INTO entity_event_types (tenant_id, category_code, category_name, category_polarity, " +
            "type_code, type_name, is_system, is_enabled, sort_order, deleted) " +
            "VALUES (1, ?, ?, ?, ?, ?, 0, 1, 0, 0)",
            categoryCode, categoryName, polarity == null ? "NEUTRAL" : polarity,
            categoryCode + "_PLACEHOLDER", categoryName + "(默认)");
        return null;
    }

    // ==================== trigger_points ====================

    public List<Map<String, Object>> listTriggerPoints(String module) {
        // L3 fix: 原 controller 用 string 拼接 + replace 单引号 — 改参数化, 防 SQL 注入.
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM trigger_points WHERE deleted = 0 AND plugin_enabled = 1");
        List<Object> params = new ArrayList<>();
        if (module != null && !module.isBlank()) {
            sql.append(" AND module_code = ?");
            params.add(module);
        }
        sql.append(" ORDER BY module_code, sort_order");
        return toCamelCaseList(jdbcTemplate.queryForList(sql.toString(), params.toArray()));
    }

    public Map<String, Object> getTriggerPointById(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM trigger_points WHERE id = ? AND deleted = 0", id);
        return rows.isEmpty() ? null : toCamelCase(rows.get(0));
    }

    @Transactional
    public void createTriggerPoint(Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO trigger_points (module_code, module_name, point_code, point_name, " +
            "description, context_schema, is_enabled, sort_order, tenant_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
            body.get("moduleCode"), body.get("moduleName"),
            body.get("pointCode"), body.get("pointName"),
            body.get("description"),
            body.get("contextSchema") != null ? body.get("contextSchema").toString() : null,
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0));
    }

    @Transactional
    public void updateTriggerPoint(Long id, Map<String, Object> body) {
        pluginEnabledGuard.check("trigger_points", id);
        jdbcTemplate.update(
            "UPDATE trigger_points SET module_code = ?, module_name = ?, point_code = ?, " +
            "point_name = ?, description = ?, context_schema = ?, sort_order = ? " +
            "WHERE id = ? AND deleted = 0",
            body.get("moduleCode"), body.get("moduleName"),
            body.get("pointCode"), body.get("pointName"),
            body.get("description"),
            body.get("contextSchema") != null ? body.get("contextSchema").toString() : null,
            body.getOrDefault("sortOrder", 0),
            id);
    }

    @Transactional
    public void deleteTriggerPoint(Long id) {
        pluginEnabledGuard.check("trigger_points", id);
        jdbcTemplate.update("UPDATE trigger_points SET deleted = 1 WHERE id = ?", id);
    }

    @Transactional
    public void enableTriggerPoint(Long id) {
        pluginEnabledGuard.check("trigger_points", id);
        jdbcTemplate.update("UPDATE trigger_points SET is_enabled = 1 WHERE id = ? AND deleted = 0", id);
    }

    @Transactional
    public void disableTriggerPoint(Long id) {
        pluginEnabledGuard.check("trigger_points", id);
        jdbcTemplate.update("UPDATE trigger_points SET is_enabled = 0 WHERE id = ? AND deleted = 0", id);
    }

    // ==================== helpers ====================

    private String toJsonString(Object value) {
        if (value == null) return null;
        if (value instanceof String) return (String) value;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
