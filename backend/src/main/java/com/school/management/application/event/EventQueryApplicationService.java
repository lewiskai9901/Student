package com.school.management.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.school.management.common.util.SnakeToCamelUtil.toCamelCaseList;

/**
 * 实体事件查询服务 (L3, 2026-05-19).
 *
 * <p>L3 收拢前 EventController 8 处直 jdbc 查询/统计 到此服务. 与
 * {@link EventConfigApplicationService} 区分: 这里是**事件流查询/聚合**,
 * 不做配置 CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventQueryApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 按主体查询事件流, 含极性/分类/类型筛选, 分页.
     */
    public Map<String, Object> queryBySubject(String subjectType, Long subjectId,
                                              String polarity, String category, String eventType,
                                              int page, int size) {
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

        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + whereSql, Integer.class, params.toArray());

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
        return result;
    }

    /**
     * 事件统计 (按主体/分类/极性聚合).
     */
    public Map<String, Object> statistics(String subjectType, Long subjectId,
                                          String startDate, String endDate) {
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

        List<Map<String, Object>> byCategory = toCamelCaseList(jdbcTemplate.queryForList(
            "SELECT e.event_category, t.category_name, t.category_polarity, COUNT(*) as count " +
            whereSql + " GROUP BY e.event_category, t.category_name, t.category_polarity " +
            "ORDER BY count DESC", params.toArray()));

        List<Map<String, Object>> byPolarity = toCamelCaseList(jdbcTemplate.queryForList(
            "SELECT COALESCE(t.category_polarity, 'NEUTRAL') as polarity, COUNT(*) as count " +
            whereSql + " GROUP BY t.category_polarity " +
            "ORDER BY count DESC", params.toArray()));

        List<Map<String, Object>> byType = toCamelCaseList(jdbcTemplate.queryForList(
            "SELECT e.event_type, t.type_name, t.category_polarity, t.icon, t.color, COUNT(*) as count " +
            whereSql + " GROUP BY e.event_type, t.type_name, t.category_polarity, t.icon, t.color " +
            "ORDER BY count DESC LIMIT 10", params.toArray()));

        Integer total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) " + whereSql, Integer.class, params.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total != null ? total : 0);
        result.put("byCategory", byCategory);
        result.put("byPolarity", byPolarity);
        result.put("byType", byType);
        return result;
    }
}
