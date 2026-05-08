package com.school.management.interfaces.rest.access;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关系变更历史查询.
 */
@RestController
@RequestMapping("/access-relations/history")
@RequiredArgsConstructor
public class AccessRelationHistoryController {

    private final JdbcTemplate jdbcTemplate;

    /** 查某 subject (人) 的所有关系变更. */
    @GetMapping("/by-subject")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> bySubject(
            @RequestParam String subjectType,
            @RequestParam Long subjectId,
            @RequestParam(defaultValue = "100") int limit) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, original_id, resource_type, resource_id, relation, " +
            "  subject_type, subject_id, access_level, valid_from, valid_to, " +
            "  archived_at, archived_reason, archived_by, operator_ip, operation " +
            "FROM access_relations_history " +
            "WHERE subject_type=? AND subject_id=? " +
            "ORDER BY archived_at DESC LIMIT ?",
            subjectType, subjectId, limit);
        return Result.success(rows);
    }

    /** 查某 resource (东西) 的所有关系变更. */
    @GetMapping("/by-resource")
    @PreAuthorize("isAuthenticated()")
    public Result<List<Map<String, Object>>> byResource(
            @RequestParam String resourceType,
            @RequestParam Long resourceId,
            @RequestParam(defaultValue = "100") int limit) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, original_id, resource_type, resource_id, relation, " +
            "  subject_type, subject_id, access_level, valid_from, valid_to, " +
            "  archived_at, archived_reason, archived_by, operator_ip, operation " +
            "FROM access_relations_history " +
            "WHERE resource_type=? AND resource_id=? " +
            "ORDER BY archived_at DESC LIMIT ?",
            resourceType, resourceId, limit);
        return Result.success(rows);
    }

    /** 最近 N 天变更 (审计页用). */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('system:audit:view')")
    public Result<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "200") int limit) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, resource_type, resource_id, relation, " +
            "  subject_type, subject_id, archived_at, archived_reason, archived_by, operation " +
            "FROM access_relations_history " +
            "WHERE archived_at >= NOW() - INTERVAL ? DAY " +
            "ORDER BY archived_at DESC LIMIT ?",
            days, limit);
        return Result.success(rows);
    }
}
