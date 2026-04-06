package com.school.management.interfaces.rest.event;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.school.management.common.util.SnakeToCamelUtil.toCamelCase;
import static com.school.management.common.util.SnakeToCamelUtil.toCamelCaseList;

/**
 * 事件类型管理 API (增强版，路径 /event/types)
 * 支持按分类分组、分类极性、分类CRUD
 */
@RestController
@RequestMapping("/event/types")
@Tag(name = "事件类型管理(增强)", description = "事件类型配置 API - 按分类分组，含极性")
@RequiredArgsConstructor
public class EventTypeController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "获取事件类型列表（按分类分组）")
    @CasbinAccess(resource = "entity-event-type", action = "view")
    public Result<List<Map<String, Object>>> listGrouped(
            @RequestParam(required = false) String category) {
        String sql = "SELECT * FROM entity_event_types WHERE deleted = 0";
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isBlank()) {
            sql += " AND category_code = ?";
            params.add(category);
        }
        sql += " ORDER BY sort_order";

        List<Map<String, Object>> allTypes = jdbcTemplate.queryForList(sql, params.toArray());

        // Group by category (use raw snake_case keys for grouping, then convert)
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
        return Result.success(result);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取事件分类列表（含极性）")
    @CasbinAccess(resource = "entity-event-type", action = "view")
    public Result<List<Map<String, Object>>> listCategories() {
        List<Map<String, Object>> categories = jdbcTemplate.queryForList(
            "SELECT DISTINCT category_code, category_name, category_polarity " +
            "FROM entity_event_types WHERE deleted = 0 " +
            "ORDER BY MIN(sort_order)");
        // The above DISTINCT won't work with ORDER BY MIN. Use a subquery approach.
        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "SELECT category_code, category_name, category_polarity, " +
            "COUNT(*) as type_count, MIN(sort_order) as min_sort " +
            "FROM entity_event_types WHERE deleted = 0 " +
            "GROUP BY category_code, category_name, category_polarity " +
            "ORDER BY min_sort");
        return Result.success(toCamelCaseList(result));
    }

    @PostMapping
    @Operation(summary = "创建事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "add")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        // Determine the category_polarity from existing category or from body
        String categoryPolarity = (String) body.getOrDefault("categoryPolarity", "NEUTRAL");
        if (body.get("categoryCode") != null) {
            try {
                String existingPolarity = jdbcTemplate.queryForObject(
                    "SELECT category_polarity FROM entity_event_types WHERE category_code = ? AND deleted = 0 LIMIT 1",
                    String.class, body.get("categoryCode"));
                if (existingPolarity != null) {
                    categoryPolarity = existingPolarity;
                }
            } catch (Exception ignored) {}
        }

        jdbcTemplate.update(
            "INSERT INTO entity_event_types (tenant_id, category_code, category_name, category_polarity, " +
            "type_code, type_name, icon, color, applicable_subjects, " +
            "is_system, is_enabled, sort_order) " +
            "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            body.get("categoryCode"), body.get("categoryName"), categoryPolarity,
            body.get("typeCode"), body.get("typeName"),
            body.get("icon"), body.get("color"),
            body.get("applicableSubjects"),
            body.getOrDefault("isSystem", 0),
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0));
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "UPDATE entity_event_types SET category_code = ?, category_name = ?, category_polarity = ?, " +
            "type_name = ?, icon = ?, color = ?, applicable_subjects = ?, " +
            "is_enabled = ?, sort_order = ? " +
            "WHERE id = ? AND deleted = 0",
            body.get("categoryCode"), body.get("categoryName"),
            body.getOrDefault("categoryPolarity", "NEUTRAL"),
            body.get("typeName"),
            body.get("icon"), body.get("color"),
            body.get("applicableSubjects"),
            body.getOrDefault("isEnabled", 1),
            body.getOrDefault("sortOrder", 0),
            id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除事件类型")
    @CasbinAccess(resource = "entity-event-type", action = "delete")
    public Result<Void> delete(@PathVariable Long id) {
        // Check if it's a system type
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT is_system FROM entity_event_types WHERE id = ? AND deleted = 0", id);
        if (!rows.isEmpty()) {
            Object isSystem = rows.get(0).get("is_system");
            if (isSystem != null && (Integer.valueOf(1).equals(isSystem) || Boolean.TRUE.equals(isSystem))) {
                return Result.error("系统预置类型不允许删除");
            }
        }
        jdbcTemplate.update("UPDATE entity_event_types SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/categories")
    @Operation(summary = "创建事件分类（批量插入空分类占位）")
    @CasbinAccess(resource = "entity-event-type", action = "add")
    public Result<Void> createCategory(@RequestBody Map<String, Object> body) {
        String categoryCode = (String) body.get("categoryCode");
        String categoryName = (String) body.get("categoryName");
        String polarity = (String) body.getOrDefault("categoryPolarity", "NEUTRAL");
        if (categoryCode == null || categoryName == null) {
            return Result.error("categoryCode和categoryName不能为空");
        }
        // Check if category already exists
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM entity_event_types WHERE category_code = ? AND deleted = 0",
            Integer.class, categoryCode);
        if (count != null && count > 0) {
            return Result.error("分类编码已存在: " + categoryCode);
        }
        // Insert a placeholder type so the category appears in queries
        jdbcTemplate.update(
            "INSERT INTO entity_event_types (tenant_id, category_code, category_name, category_polarity, " +
            "type_code, type_name, is_system, is_enabled, sort_order, deleted) " +
            "VALUES (1, ?, ?, ?, ?, ?, 0, 1, 0, 0)",
            categoryCode, categoryName, polarity,
            categoryCode + "_PLACEHOLDER", categoryName + "(默认)");
        return Result.success(null);
    }
}
