package com.school.management.application.inspection;

import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 整改建议引擎相关数据访问 — 从 CorrectiveSuggestionController 抽离的 jdbc 持久化逻辑.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorrectiveSuggestionApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    /** 写入 V110 引擎字段到 corrective_case (失败静默忽略, 保持原行为). */
    @Transactional
    public void writeEngineFields(Long caseId, String suggestionReason,
                                  double severityScore, String explainTraceJson) {
        try {
            jdbcTemplate.update(
                    "UPDATE insp_corrective_cases SET suggested_by_system=1, " +
                    " suggestion_reason=?, severity_score=?, explain_trace_json=? " +
                    " WHERE id=?",
                    suggestionReason,
                    severityScore,
                    explainTraceJson,
                    caseId);
        } catch (Exception e) {
            // P2#20: V110 引擎字段是非关键增强数据 (建议理由 / 严重度分 / 解释链),
            // 写失败不应影响主整改流程, 故吞掉; 但必须留痕便于排查引擎字段缺失.
            log.debug("写入 V110 引擎字段失败 (非关键, 已忽略): caseId={}, msg={}",
                    caseId, e.getMessage());
        }
    }

    /** 更新项目整改策略 (V20260524_7 重构: 加新字段 enabled / strictnessAdj / autoCreateLevel). */
    @Transactional
    public void updateProjectPolicy(Long projectId,
                                    Boolean enabled, Integer strictnessAdj, String autoCreateLevel,
                                    String thresholdsJson, String deadlinesJson) {
        // 旧 strictness 列保留为兼容数据 (loadPolicy 回退使用), 暂不主动覆盖
        jdbcTemplate.update(
                "UPDATE insp_projects SET corrective_enabled=?, " +
                " corrective_strictness_adj=?, corrective_auto_create_level=?, " +
                " corrective_severity_thresholds=?, corrective_default_deadlines=? " +
                " WHERE id=?",
                enabled == null ? 1 : (enabled ? 1 : 0),
                strictnessAdj == null ? 0 : strictnessAdj,
                autoCreateLevel,
                thresholdsJson, deadlinesJson, projectId);
    }

    /** 拉取某主体过去 30 天每个 itemCode 的复发计数. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queryRecurrence(Long projectId, Long subjectId) {
        return jdbcTemplate.queryForList(
                "SELECT d.item_code AS itemCode, " +
                "       MAX(d.item_name) AS itemName, " +
                "       COUNT(DISTINCT d.submission_id) AS recurCount, " +
                "       MAX(s.created_at) AS lastSeenAt " +
                "  FROM insp_submission_details d " +
                "  JOIN insp_submissions s ON d.submission_id = s.id " +
                "  JOIN insp_tasks t ON s.task_id = t.id " +
                " WHERE t.project_id = ? AND s.target_id = ? " +
                "   AND d.deleted = 0 AND s.deleted = 0 " +
                "   AND s.created_at >= NOW() - INTERVAL 30 DAY " +
                "   AND ( UPPER(d.response_value) IN ('FAIL','D','NO','FALSE','0') " +
                "      OR d.score < 0 " +
                "      OR (d.score IS NOT NULL AND d.item_weight > 0 " +
                "          AND d.score / d.item_weight < 0.5) " +
                "      OR d.is_flagged = 1 ) " +
                scopeHelper.orgScopeClause("t.org_unit_id") +
                " GROUP BY d.item_code " +
                "HAVING recurCount >= 1",
                projectId, subjectId);
    }

    /** 引擎 KPI — 系统/人工占比 + severity 分布 + 关闭率 + 平均严重度 + Top10 复发. */
    @Transactional(readOnly = true)
    public EngineKpiData queryEngineKpi(Long projectId) {
        EngineKpiData kpi = new EngineKpiData();
        String scopeClause = scopeHelper.orgScopeClause("org_unit_id");
        String pidFilter = (projectId == null ? "" : " AND project_id = " + projectId.longValue()) + scopeClause;

        // 1. 系统建议 vs 人工
        Map<String, Object> srcRow = jdbcTemplate.queryForMap(
                "SELECT " +
                "  SUM(CASE WHEN suggested_by_system = 1 THEN 1 ELSE 0 END) AS engine_cnt, " +
                "  SUM(CASE WHEN suggested_by_system <> 1 OR suggested_by_system IS NULL THEN 1 ELSE 0 END) AS manual_cnt, " +
                "  COUNT(*) AS total_cnt " +
                "  FROM insp_corrective_cases " +
                " WHERE deleted = 0" + pidFilter);
        kpi.engineCount = num(srcRow.get("engine_cnt"));
        kpi.manualCount = num(srcRow.get("manual_cnt"));
        kpi.totalCount = num(srcRow.get("total_cnt"));

        // 2. severity 分布
        List<Map<String, Object>> sevRows = jdbcTemplate.queryForList(
                "SELECT priority, COUNT(*) AS c " +
                "  FROM insp_corrective_cases " +
                " WHERE deleted = 0 AND suggested_by_system = 1" + pidFilter +
                " GROUP BY priority");
        kpi.severityDist = new HashMap<>();
        for (Map<String, Object> r : sevRows) {
            kpi.severityDist.put((String) r.get("priority"), num(r.get("c")));
        }

        // 3. 关闭率
        Map<String, Object> closeRow = jdbcTemplate.queryForMap(
                "SELECT " +
                "  SUM(CASE WHEN status IN ('CLOSED','VERIFIED') THEN 1 ELSE 0 END) AS closed, " +
                "  COUNT(*) AS total " +
                "  FROM insp_corrective_cases WHERE deleted = 0" + pidFilter);
        kpi.closedCount = num(closeRow.get("closed"));
        kpi.closeTotalCount = num(closeRow.get("total"));

        // 4. 平均严重度分
        kpi.avgSeverityScore = jdbcTemplate.queryForObject(
                "SELECT AVG(severity_score) FROM insp_corrective_cases " +
                " WHERE deleted = 0 AND severity_score IS NOT NULL" + pidFilter,
                Double.class);

        // 5. Top10 复发 itemCode
        String pidJoin = (projectId == null ? "" : " AND t.project_id = " + projectId.longValue())
                + scopeHelper.orgScopeClause("t.org_unit_id");
        kpi.topRecurring = jdbcTemplate.queryForList(
                "SELECT d.item_code AS itemCode, MAX(d.item_name) AS itemName, " +
                "       COUNT(DISTINCT d.submission_id) AS recur " +
                "  FROM insp_submission_details d " +
                "  JOIN insp_submissions s ON d.submission_id = s.id " +
                "  JOIN insp_tasks t ON s.task_id = t.id " +
                " WHERE d.deleted = 0 AND s.deleted = 0 " +
                "   AND s.created_at >= NOW() - INTERVAL 30 DAY " +
                "   AND ( UPPER(d.response_value) IN ('FAIL','D','NO','FALSE','0') " +
                "      OR d.score < 0 " +
                "      OR (d.score IS NOT NULL AND d.item_weight > 0 AND d.score / d.item_weight < 0.5) " +
                "      OR d.is_flagged = 1 ) " +
                pidJoin +
                " GROUP BY d.item_code " +
                " ORDER BY recur DESC LIMIT 10");
        return kpi;
    }

    /** 取检查项整改覆盖规则 JSON. 查询失败返回 null (保持原行为). */
    @Transactional(readOnly = true)
    public String getItemOverride(Long itemId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT corrective_override FROM insp_template_items WHERE id=?",
                    String.class, itemId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 更新检查项整改覆盖规则 JSON. */
    @Transactional
    public void updateItemOverride(Long itemId, String json) {
        jdbcTemplate.update(
                "UPDATE insp_template_items SET corrective_override=? WHERE id=?",
                json, itemId);
    }

    private static long num(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    /** 引擎 KPI 原始数据 — 由 controller 组装为展示用 EngineKpi. */
    public static class EngineKpiData {
        public long engineCount;
        public long manualCount;
        public long totalCount;
        public Map<String, Long> severityDist;
        public long closedCount;
        public long closeTotalCount;
        public Double avgSeverityScore;
        public List<Map<String, Object>> topRecurring;
    }
}
