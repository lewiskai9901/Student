package com.school.management.application.inspection;

import com.school.management.application.access.AccessRelationApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 受检主体面数据访问 — 从 MyReceivedInspectionsController 抽离的 jdbc 持久化逻辑.
 */
@Service
@RequiredArgsConstructor
public class MyReceivedInspectionsApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final AccessRelationApplicationService accessRelationApplicationService;

    /** 解析指定用户所在的 org_unit ID 列表 (走 access 应用服务, 不直访 repository). */
    public List<Long> resolveOrgUnitIds(Long userId) {
        return accessRelationApplicationService.resolveOrgUnitIds(userId);
    }

    /** 历史检查记录 — 按时间倒序. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryInspections(List<Long> myOrgs, Long projectId, int days) {
        StringBuilder sql = new StringBuilder(
                "SELECT s.id AS submissionId, s.created_at AS inspectedAt, " +
                "  s.target_id AS subjectId, s.target_name AS subjectName, " +
                "  s.final_score AS score, NULL AS maxScore, s.status AS submissionStatus, " +
                "  t.id AS taskId, t.task_code AS taskCode, " +
                "  p.id AS projectId, p.project_name AS projectName, " +
                "  (SELECT COUNT(*) FROM insp_submission_details d " +
                "    WHERE d.submission_id = s.id AND d.deleted = 0 AND d.is_flagged = 1) AS issueCount " +
                "  FROM insp_submissions s " +
                "  JOIN insp_tasks t ON s.task_id = t.id " +
                "  JOIN insp_projects p ON t.project_id = p.id " +
                " WHERE s.deleted = 0 AND t.deleted = 0 " +
                "   AND s.target_id IN (");
        for (int i = 0; i < myOrgs.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(") ");
        sql.append(" AND s.created_at >= NOW() - INTERVAL ").append(days).append(" DAY ");
        if (projectId != null) sql.append(" AND p.id = ").append(projectId.longValue()).append(" ");
        sql.append(" ORDER BY s.created_at DESC LIMIT 200");

        return jdbcTemplate.queryForList(sql.toString(), myOrgs.toArray());
    }

    /** 4 周趋势 — 按周聚合. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryTrends(List<Long> myOrgs, int weeks) {
        StringBuilder sql = new StringBuilder(
                "SELECT YEARWEEK(s.created_at, 3) AS isoWeek, " +
                "  DATE(MIN(s.created_at)) AS weekStart, " +
                "  COUNT(s.id) AS submissionCount, " +
                "  ROUND(AVG(s.final_score), 2) AS avgScore, " +
                "  NULL AS avgPct, " +
                "  SUM((SELECT COUNT(*) FROM insp_submission_details d " +
                "    WHERE d.submission_id = s.id AND d.deleted = 0 AND d.is_flagged = 1)) AS totalIssues " +
                "  FROM insp_submissions s " +
                " WHERE s.deleted = 0 " +
                "   AND s.target_id IN (");
        for (int i = 0; i < myOrgs.size(); i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(") ");
        sql.append(" AND s.created_at >= NOW() - INTERVAL ").append(weeks).append(" WEEK ");
        sql.append(" GROUP BY YEARWEEK(s.created_at, 3) ORDER BY isoWeek ASC");

        return jdbcTemplate.queryForList(sql.toString(), myOrgs.toArray());
    }

    /** Top N 反复出问题的检查项 (近 30 天). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryRecurring(List<Long> myOrgs, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT d.item_code AS itemCode, MAX(d.item_name) AS itemName, " +
                "  MAX(d.section_name) AS sectionName, " +
                "  COUNT(DISTINCT d.submission_id) AS recurCount, " +
                "  MAX(s.created_at) AS lastSeenAt " +
                "  FROM insp_submission_details d " +
                "  JOIN insp_submissions s ON d.submission_id = s.id " +
                " WHERE d.deleted = 0 AND s.deleted = 0 " +
                "   AND s.created_at >= NOW() - INTERVAL 30 DAY " +
                "   AND s.target_id IN (");
        for (int i = 0; i < myOrgs.size(); i++) sql.append(i == 0 ? "?" : ",?");
        sql.append(") ");
        sql.append(" AND ( UPPER(d.response_value) IN ('FAIL','D','NO','FALSE','0') ");
        sql.append("    OR d.score < 0 ");
        sql.append("    OR (d.score IS NOT NULL AND d.item_weight > 0 ");
        sql.append("        AND d.score / d.item_weight < 0.5) ");
        sql.append("    OR d.is_flagged = 1 ) ");
        sql.append(" GROUP BY d.item_code HAVING recurCount >= 1 ");
        sql.append(" ORDER BY recurCount DESC, lastSeenAt DESC LIMIT ").append(limit);

        return jdbcTemplate.queryForList(sql.toString(), myOrgs.toArray());
    }

    /** 统计概览 — 检查次数 / 平均分. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> querySummaryInspections(List<Long> myOrgs, int days) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < myOrgs.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
        return jdbcTemplate.queryForList(
                "SELECT COUNT(s.id) AS totalInspections, " +
                "  ROUND(AVG(s.final_score), 2) AS avgScore, " +
                "  NULL AS avgPct " +
                "  FROM insp_submissions s " +
                " WHERE s.deleted = 0 " +
                "   AND s.target_id IN (" + placeholders + ") " +
                "   AND s.created_at >= NOW() - INTERVAL " + days + " DAY",
                myOrgs.toArray());
    }

    /** 统计概览 — 整改未关闭 / 逾期数. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> querySummaryCorrectives(List<Long> myOrgs) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < myOrgs.size(); i++) placeholders.append(i == 0 ? "?" : ",?");
        return jdbcTemplate.queryForList(
                "SELECT " +
                "  SUM(CASE WHEN status NOT IN ('CLOSED','VERIFIED') THEN 1 ELSE 0 END) AS openCorrectives, " +
                "  SUM(CASE WHEN status NOT IN ('CLOSED','VERIFIED') AND deadline IS NOT NULL AND deadline < NOW() THEN 1 ELSE 0 END) AS overdueCorrectives " +
                "  FROM insp_corrective_cases " +
                " WHERE deleted = 0 AND target_id IN (" + placeholders + ")",
                myOrgs.toArray());
    }
}
