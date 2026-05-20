package com.school.management.application.inspection;

import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 检查平台数据导出查询 — 从 InspectionExportController 抽离的 jdbc 持久化逻辑.
 * 仅做数据访问; Excel 构建仍留在 controller (展示层).
 */
@Service
@RequiredArgsConstructor
public class InspectionExportApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    /** 周期排名数据. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryRanking(Long projectId, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder("""
            SELECT summary_date, target_name, org_unit_name,
                   inspection_count, avg_score, min_score, max_score,
                   total_deductions, ranking, grade
            FROM insp_daily_summaries
            WHERE summary_date BETWEEN ? AND ? AND deleted = 0
            """);
        Object[] params = projectId != null
                ? new Object[]{start, end, projectId}
                : new Object[]{start, end};
        if (projectId != null) sql.append(" AND project_id = ?");
        sql.append(scopeHelper.orgScopeClause("org_unit_id"));   // I1: 旁路加数据权限
        sql.append(" ORDER BY summary_date DESC, ranking ASC");

        return jdbcTemplate.queryForList(sql.toString(), params);
    }

    /** 整改履约数据. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryCorrective(Long projectId, String status) {
        StringBuilder sql = new StringBuilder("""
            SELECT case_code, target_name, issue_description, priority, status,
                   deadline, assignee_name, escalation_level, created_at, verified_at
            FROM insp_corrective_cases
            WHERE deleted = 0
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (projectId != null) { sql.append(" AND project_id = ?"); params.add(projectId); }
        if (status != null) { sql.append(" AND status = ?"); params.add(status); }
        sql.append(scopeHelper.orgScopeClause("org_unit_id"));   // I1
        sql.append(" ORDER BY created_at DESC LIMIT 5000");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /** 申诉处理记录. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryAppeals() {
        // I1: 旁路加数据权限
        return jdbcTemplate.queryForList(
            "SELECT appeal_code, submitter_name, reason, status, expected_adjustment, " +
            "       final_adjustment, reviewer_name, reviewer_comment, " +
            "       created_at, reviewed_at " +
            "FROM inspection_appeals " +
            "WHERE deleted = 0" + scopeHelper.orgScopeClause("org_unit_id") +
            " ORDER BY created_at DESC LIMIT 5000");
    }

    /** 审计日志数据. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryAudit(String aggregateType) {
        StringBuilder sql = new StringBuilder("""
            SELECT occurred_at, entity_type, entity_code, action,
                   actor_user_name, reason
            FROM inspection_audit_logs
            WHERE 1=1
            """);
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (aggregateType != null) { sql.append(" AND entity_type = ?"); params.add(aggregateType); }
        // I4: 加 org_unit_id scope (允许 NULL 通过, 兼容历史无 org 数据)
        var ids = scopeHelper.allowedOrgIds();
        if (ids != null) {
            if (ids.isEmpty()) {
                sql.append(" AND 1=0");
            } else {
                String csv = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
                sql.append(" AND (org_unit_id IS NULL OR org_unit_id IN (").append(csv).append("))");
            }
        }
        sql.append(" ORDER BY occurred_at DESC LIMIT 5000");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }
}
