package com.school.management.interfaces.rest.access;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 关系字典 REST API (v3)
 * GET  /api/relation-types          列出所有关系类型
 * GET  /api/relation-types/tiers    按 tier 分组
 *
 * 响应字段统一 camelCase,与前端 TS 约定对齐。
 */
@Slf4j
@RestController
@RequestMapping("/relation-types")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")  // Phase 6.7: 关系字典只读公共数据, 要求登录即可
public class RelationTypeController {

    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_ALL =
        "SELECT relation_code, from_type, to_type, relation_name, is_transitive, " +
        "       category, tier, registered_by, description, capacity_bound, max_per_resource, max_by_subtype, " +
        "       industry, plugin_class, origin, is_enabled " +
        "FROM relation_types WHERE is_enabled = 1";

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String fromType,
            @RequestParam(required = false) String toType) {
        StringBuilder sql = new StringBuilder(SELECT_ALL);
        List<Object> params = new ArrayList<>();
        if (tier != null && !tier.isBlank()) {
            sql.append(" AND tier = ?");
            params.add(tier);
        }
        if (fromType != null && !fromType.isBlank()) {
            sql.append(" AND from_type = ?");
            params.add(fromType);
        }
        if (toType != null && !toType.isBlank()) {
            sql.append(" AND to_type = ?");
            params.add(toType);
        }
        sql.append(" ORDER BY tier, category, relation_code");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return Result.success(rows.stream().map(this::toCamelCase).toList());
    }

    @GetMapping("/tiers")
    public Result<Map<String, List<Map<String, Object>>>> listByTier() {
        List<Map<String, Object>> all = jdbcTemplate.queryForList(
            SELECT_ALL + " ORDER BY tier, category, relation_code");
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : all) {
            String tier = (String) row.get("tier");
            grouped.computeIfAbsent(tier, k -> new ArrayList<>()).add(toCamelCase(row));
        }
        return Result.success(grouped);
    }

    /** snake_case -> camelCase 字段名转换;max_by_subtype JSON 字符串解析为对象 */
    private Map<String, Object> toCamelCase(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        row.forEach((k, v) -> {
            String key = snakeToCamel(k);
            if ("maxBySubtype".equals(key) && v instanceof String s && !s.isBlank()) {
                try { out.put(key, MAPPER.readValue(s, Map.class)); }
                catch (Exception e) { out.put(key, null); }
            } else {
                out.put(key, v);
            }
        });
        return out;
    }

    private String snakeToCamel(String s) {
        if (s == null || !s.contains("_")) return s;
        StringBuilder sb = new StringBuilder(s.length());
        boolean upper = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }
}
