package com.school.management.interfaces.rest.event;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.school.management.common.util.SnakeToCamelUtil.toCamelCase;
import static com.school.management.common.util.SnakeToCamelUtil.toCamelCaseList;

/**
 * 实体事件查询 API (增强版，路径 /event/events)
 * 支持按主体+极性查询、统计聚合
 */
@RestController
@RequestMapping("/event/events")
@Tag(name = "实体事件查询(增强)", description = "事件查询 API - 含极性筛选、统计聚合")
@RequiredArgsConstructor
public class EventController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/by-subject")
    @Operation(summary = "按主体查询事件流（支持极性筛选和分页）")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<Map<String, Object>> getBySubject(
            @RequestParam String subjectType,
            @RequestParam Long subjectId,
            @RequestParam(required = false) String polarity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder whereSql = new StringBuilder(
            "FROM entity_events e " +
            "LEFT JOIN entity_event_types t ON e.event_type = t.type_code AND t.deleted = 0 " +
            "WHERE e.deleted = 0 AND e.subject_type = ? AND e.subject_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(subjectType);
        params.add(subjectId);

        if (polarity != null && !polarity.isBlank()) {
            whereSql.append(" AND t.category_polarity = ?");
            params.add(polarity);
        }
        if (category != null && !category.isBlank()) {
            whereSql.append(" AND e.event_category = ?");
            params.add(category);
        }
        if (eventType != null && !eventType.isBlank()) {
            whereSql.append(" AND e.event_type = ?");
            params.add(eventType);
        }

        // Count
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + whereSql, Integer.class, params.toArray());

        // Query with pagination
        int offset = (page - 1) * size;
        String dataSql = "SELECT e.*, " +
            "t.type_name, t.category_name, t.category_polarity, t.icon AS type_icon, t.color AS type_color " +
            whereSql + " ORDER BY e.occurred_at DESC LIMIT ? OFFSET ?";
        params.add(size);
        params.add(offset);

        List<Map<String, Object>> records = toCamelCaseList(jdbcTemplate.queryForList(dataSql, params.toArray()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "事件统计（按主体/分类/极性聚合）")
    @CasbinAccess(resource = "entity-event", action = "view")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) String subjectType,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        StringBuilder whereSql = new StringBuilder(
            "FROM entity_events e " +
            "LEFT JOIN entity_event_types t ON e.event_type = t.type_code AND t.deleted = 0 " +
            "WHERE e.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (subjectType != null && !subjectType.isBlank()) {
            whereSql.append(" AND e.subject_type = ?");
            params.add(subjectType);
        }
        if (subjectId != null) {
            whereSql.append(" AND e.subject_id = ?");
            params.add(subjectId);
        }
        if (startDate != null && !startDate.isBlank()) {
            whereSql.append(" AND e.occurred_at >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            whereSql.append(" AND e.occurred_at <= ?");
            params.add(endDate);
        }

        // By category
        List<Map<String, Object>> byCategory = toCamelCaseList(jdbcTemplate.queryForList(
            "SELECT e.event_category, t.category_name, t.category_polarity, COUNT(*) as count " +
            whereSql + " GROUP BY e.event_category, t.category_name, t.category_polarity " +
            "ORDER BY count DESC", params.toArray()));

        // By polarity
        List<Map<String, Object>> byPolarity = toCamelCaseList(jdbcTemplate.queryForList(
            "SELECT COALESCE(t.category_polarity, 'NEUTRAL') as polarity, COUNT(*) as count " +
            whereSql + " GROUP BY t.category_polarity " +
            "ORDER BY count DESC", params.toArray()));

        // By event type (top 10)
        List<Map<String, Object>> byType = toCamelCaseList(jdbcTemplate.queryForList(
            "SELECT e.event_type, t.type_name, t.category_polarity, t.icon, t.color, COUNT(*) as count " +
            whereSql + " GROUP BY e.event_type, t.type_name, t.category_polarity, t.icon, t.color " +
            "ORDER BY count DESC LIMIT 10", params.toArray()));

        // Total
        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + whereSql, Integer.class, params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total != null ? total : 0);
        result.put("byCategory", byCategory);
        result.put("byPolarity", byPolarity);
        result.put("byType", byType);

        return Result.success(result);
    }
}
