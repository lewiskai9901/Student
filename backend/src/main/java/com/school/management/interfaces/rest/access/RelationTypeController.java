package com.school.management.interfaces.rest.access;

import com.school.management.application.access.RelationTypeJdbcApplicationService;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final RelationTypeJdbcApplicationService relationTypeService;

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    @GetMapping
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String fromType,
            @RequestParam(required = false) String toType,
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        List<Map<String, Object>> rows = relationTypeService.list(
            tier, fromType, toType, Boolean.TRUE.equals(includeDisabled));
        return Result.success(rows.stream().map(this::toCamelCase).toList());
    }

    @GetMapping("/tiers")
    public Result<Map<String, List<Map<String, Object>>>> listByTier(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        List<Map<String, Object>> all = relationTypeService.listAllOrdered(
            Boolean.TRUE.equals(includeDisabled));
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
            } else if ("impliedRelations".equals(key) && v instanceof String s && !s.isBlank()) {
                try { out.put(key, MAPPER.readValue(s, List.class)); }
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
