package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 受检主体面 (Subject-side view) — 班主任 / 场所负责人 / 受检单位的"被检查"视角.
 *
 * <ul>
 *   <li>GET /received/inspections — 我所在组织被检查的全部历史</li>
 *   <li>GET /received/trends      — 4 周趋势 (按周聚合扣分)</li>
 *   <li>GET /received/recurring   — Top N 反复出问题的检查项</li>
 * </ul>
 *
 * <p>"我所在组织"通过 access_relations (subject=USER, relation=MEMBER_OF, resourceType=ORG_UNIT)
 * 推导. 如未挂任何 ORG_UNIT,返回空结果.
 */
@RestController
@RequestMapping("/inspection/received")
@RequiredArgsConstructor
public class MyReceivedInspectionsController {

    private final JdbcTemplate jdbcTemplate;
    private final AccessRelationRepository accessRelationRepository;

    /** 解析当前用户所在的 org_unit ID 列表. */
    private List<Long> myOrgUnitIds() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) return List.of();
        List<AccessRelation> rels = accessRelationRepository.findBySubjectAndResourceType(
                "USER", userId, "ORG_UNIT");
        if (rels.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>(rels.size());
        for (AccessRelation r : rels) {
            if ("MEMBER_OF".equalsIgnoreCase(r.getRelation())
                || "OWNER_OF".equalsIgnoreCase(r.getRelation())
                || "MANAGES".equalsIgnoreCase(r.getRelation())) {
                ids.add(r.getResourceId());
            }
        }
        return ids;
    }

    /** 历史检查记录 — 按时间倒序. */
    @GetMapping("/inspections")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<List<Map<String, Object>>> myInspections(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "30") int days) {
        List<Long> myOrgs = myOrgUnitIds();
        if (myOrgs.isEmpty()) return Result.success(List.of());

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

        return Result.success(jdbcTemplate.queryForList(sql.toString(), myOrgs.toArray()));
    }

    /** 4 周趋势 — 按周聚合 (avg(score)/total/issueCount). */
    @GetMapping("/trends")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<List<Map<String, Object>>> myTrends(
            @RequestParam(defaultValue = "4") int weeks) {
        List<Long> myOrgs = myOrgUnitIds();
        if (myOrgs.isEmpty()) return Result.success(List.of());
        if (weeks < 1) weeks = 4;
        if (weeks > 26) weeks = 26;

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

        return Result.success(jdbcTemplate.queryForList(sql.toString(), myOrgs.toArray()));
    }

    /** Top N 反复出问题的检查项 (近 30 天). */
    @GetMapping("/recurring")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<List<Map<String, Object>>> myRecurring(
            @RequestParam(defaultValue = "10") int limit) {
        List<Long> myOrgs = myOrgUnitIds();
        if (myOrgs.isEmpty()) return Result.success(List.of());
        if (limit < 1) limit = 10;
        if (limit > 50) limit = 50;

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

        return Result.success(jdbcTemplate.queryForList(sql.toString(), myOrgs.toArray()));
    }

    /** 统计概览 — 顶部 KPI. */
    @GetMapping("/summary")
    @CasbinAccess(resource = "insp:received", action = "view")
    public Result<Map<String, Object>> mySummary(
            @RequestParam(defaultValue = "30") int days) {
        List<Long> myOrgs = myOrgUnitIds();
        Map<String, Object> out = new HashMap<>();
        out.put("orgUnitCount", myOrgs.size());
        out.put("orgUnitIds", myOrgs);
        if (myOrgs.isEmpty()) {
            out.put("totalInspections", 0);
            out.put("avgScore", null);
            out.put("openCorrectives", 0);
            out.put("overdueCorrectives", 0);
            return Result.success(out);
        }

        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < myOrgs.size(); i++) placeholders.append(i == 0 ? "?" : ",?");

        // 用 queryForList 避免 0 行抛 EmptyResultDataAccessException
        List<Map<String, Object>> inspRows = jdbcTemplate.queryForList(
                "SELECT COUNT(s.id) AS totalInspections, " +
                "  ROUND(AVG(s.final_score), 2) AS avgScore, " +
                "  NULL AS avgPct " +
                "  FROM insp_submissions s " +
                " WHERE s.deleted = 0 " +
                "   AND s.target_id IN (" + placeholders + ") " +
                "   AND s.created_at >= NOW() - INTERVAL " + days + " DAY",
                myOrgs.toArray());
        if (!inspRows.isEmpty()) {
            out.putAll(inspRows.get(0));
        } else {
            out.put("totalInspections", 0);
            out.put("avgScore", null);
            out.put("avgPct", null);
        }

        List<Map<String, Object>> csRows = jdbcTemplate.queryForList(
                "SELECT " +
                "  SUM(CASE WHEN status NOT IN ('CLOSED','VERIFIED') THEN 1 ELSE 0 END) AS openCorrectives, " +
                "  SUM(CASE WHEN status NOT IN ('CLOSED','VERIFIED') AND deadline IS NOT NULL AND deadline < NOW() THEN 1 ELSE 0 END) AS overdueCorrectives " +
                "  FROM insp_corrective_cases " +
                " WHERE deleted = 0 AND target_id IN (" + placeholders + ")",
                myOrgs.toArray());
        if (!csRows.isEmpty()) {
            Map<String, Object> cs = csRows.get(0);
            out.put("openCorrectives", cs.get("openCorrectives") != null ? cs.get("openCorrectives") : 0);
            out.put("overdueCorrectives", cs.get("overdueCorrectives") != null ? cs.get("overdueCorrectives") : 0);
        } else {
            out.put("openCorrectives", 0);
            out.put("overdueCorrectives", 0);
        }

        return Result.success(out);
    }
}
