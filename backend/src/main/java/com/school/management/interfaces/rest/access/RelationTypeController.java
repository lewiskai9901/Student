package com.school.management.interfaces.rest.access;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 关系字典 REST API (v3)
 * GET  /api/relation-types          列出所有关系类型
 * GET  /api/relation-types/tiers    按 tier 分组
 */
@Slf4j
@RestController
@RequestMapping("/relation-types")
@RequiredArgsConstructor
public class RelationTypeController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String fromType,
            @RequestParam(required = false) String toType) {
        StringBuilder sql = new StringBuilder(
            "SELECT relation_code, from_type, to_type, relation_name, is_transitive, " +
            "       category, tier, registered_by, description, is_enabled " +
            "FROM relation_types WHERE is_enabled = 1");
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
        return Result.success(rows);
    }

    @GetMapping("/tiers")
    public Result<Map<String, List<Map<String, Object>>>> listByTier() {
        List<Map<String, Object>> all = jdbcTemplate.queryForList(
            "SELECT relation_code, from_type, to_type, relation_name, is_transitive, " +
            "       category, tier, registered_by, description " +
            "FROM relation_types WHERE is_enabled = 1 " +
            "ORDER BY tier, category, relation_code");
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> row : all) {
            String tier = (String) row.get("tier");
            grouped.computeIfAbsent(tier, k -> new ArrayList<>()).add(row);
        }
        return Result.success(grouped);
    }
}
