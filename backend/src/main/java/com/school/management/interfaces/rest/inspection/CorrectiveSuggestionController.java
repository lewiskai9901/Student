package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.CorrectiveCaseApplicationService;
import com.school.management.application.inspection.CorrectiveSuggestionService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.correction.CorrectionVerdict;
import com.school.management.domain.inspection.correction.Severity;
import com.school.management.domain.inspection.model.corrective.CasePriority;
import com.school.management.domain.inspection.model.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 整改判定引擎 — 候选与确认 API.
 *
 * <p>提交完成后, 前端可调:
 * <ul>
 *   <li>GET  /inspection/corrective/candidates?submissionId=...  — 拉取引擎建议</li>
 *   <li>POST /inspection/corrective/candidates/confirm           — 批量确认建单</li>
 * </ul>
 */
@RestController
@RequestMapping("/inspection/corrective")
@RequiredArgsConstructor
public class CorrectiveSuggestionController {

    private final CorrectiveSuggestionService suggestionService;
    private final CorrectiveCaseApplicationService caseService;
    private final SubmissionDetailRepository detailRepository;
    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    /** 候选列表 — 引擎判定结果 (NONE 已过滤). */
    @GetMapping("/candidates")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<List<CandidateView>> candidates(@RequestParam Long submissionId) {
        List<CorrectionVerdict> vs = suggestionService.suggestForSubmission(submissionId);
        List<CandidateView> views = new ArrayList<>();
        for (CorrectionVerdict v : vs) {
            CandidateView cv = new CandidateView();
            cv.detailId = v.getDetailId();
            cv.itemCode = v.getItemCode();
            cv.itemName = v.getItemName();
            cv.severity = v.getSeverity().name();
            cv.severityScore = v.getSeverityScore();
            cv.mustCorrect = v.isMustCorrect();
            cv.suggestedDeadlineDays = v.getSuggestedDeadlineDays();
            cv.reason = v.getReason();
            views.add(cv);
        }
        return Result.success(views);
    }

    /** 批量确认 — 把选中的候选项落库为 corrective_case. */
    @PostMapping("/candidates/confirm")
    @CasbinAccess(resource = "insp:corrective", action = "create")
    public Result<List<Long>> confirm(@RequestBody ConfirmRequest req) {
        if (req == null || req.submissionId == null || req.detailIds == null || req.detailIds.isEmpty()) {
            throw new IllegalArgumentException("submissionId 与 detailIds 必填");
        }

        InspSubmission submission = submissionRepository.findById(req.submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在"));
        Long projectId = submission.getTaskId() == null ? null
                : taskRepository.findById(submission.getTaskId())
                        .map(InspTask::getProjectId).orElse(null);

        // 重新计算引擎结果 (避免前端传递可被篡改的 severity)
        List<CorrectionVerdict> verdicts = suggestionService.suggestForSubmission(req.submissionId);
        Map<Long, CorrectionVerdict> byDetail = new HashMap<>();
        for (CorrectionVerdict v : verdicts) byDetail.put(v.getDetailId(), v);

        List<Long> created = new ArrayList<>();
        for (Long detailId : req.detailIds) {
            CorrectionVerdict v = byDetail.get(detailId);
            if (v == null) continue;  // 该项引擎不建议, 拒绝
            SubmissionDetail d = detailRepository.findById(detailId).orElse(null);
            if (d == null) continue;

            String caseCode = "CC-" + System.currentTimeMillis() + "-" + CODE_SEQ.incrementAndGet();
            int days = v.getSuggestedDeadlineDays() > 0 ? v.getSuggestedDeadlineDays() : 7;
            CorrectiveCase saved = caseService.createCase(
                    caseCode,
                    "[" + (d.getSectionName() == null ? "" : d.getSectionName()) + "] "
                            + d.getItemName() + " — " + v.getReason(),
                    mapPriority(v.getSeverity()),
                    req.submissionId,
                    detailId,
                    projectId,
                    submission.getTaskId(),
                    "ORG",
                    submission.getTargetId(),
                    submission.getTargetName(),
                    null,
                    LocalDateTime.now().plusDays(days),
                    SecurityUtils.getCurrentUserId());

            // 写入 V110 引擎字段 (suggested_by_system / severity_score / explain_trace / suggestion_reason)
            try {
                jdbcTemplate.update(
                        "UPDATE insp_corrective_cases SET suggested_by_system=1, " +
                        " suggestion_reason=?, severity_score=?, explain_trace_json=? " +
                        " WHERE id=?",
                        truncate(v.getReason(), 500),
                        v.getSeverityScore(),
                        suggestionService.serializeTrace(v),
                        saved.getId());
            } catch (Exception ignored) {}

            created.add(saved.getId());
        }
        return Result.success(created);
    }

    private CasePriority mapPriority(Severity sev) {
        if (sev == null) return CasePriority.MEDIUM;
        switch (sev) {
            case HIGH:   return CasePriority.HIGH;
            case MEDIUM: return CasePriority.MEDIUM;
            case LOW:    return CasePriority.LOW;
            default:     return CasePriority.MEDIUM;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    public static class CandidateView {
        public Long detailId;
        public String itemCode;
        public String itemName;
        public String severity;
        public double severityScore;
        public boolean mustCorrect;
        public int suggestedDeadlineDays;
        public String reason;
    }

    public static class ConfirmRequest {
        public Long submissionId;
        public List<Long> detailIds;
    }

    // ==================== 项目策略 GET / PUT ====================

    /** 取项目整改策略. 缺省 NORMAL. */
    @GetMapping("/projects/{projectId}/policy")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<PolicyView> getPolicy(@PathVariable Long projectId) {
        var p = suggestionService.loadPolicy(projectId);
        PolicyView v = new PolicyView();
        v.strictness = p.strictness();
        v.thresholdHigh = p.thresholds().high();
        v.thresholdMedium = p.thresholds().medium();
        v.thresholdLow = p.thresholds().low();
        v.deadlineHigh = p.deadlines().high();
        v.deadlineMedium = p.deadlines().medium();
        v.deadlineLow = p.deadlines().low();
        return Result.success(v);
    }

    /** 更新项目整改策略. strictness ∈ {STRICT,NORMAL,LENIENT,OFF}. */
    @PutMapping("/projects/{projectId}/policy")
    @CasbinAccess(resource = "insp:project", action = "update")
    public Result<PolicyView> updatePolicy(@PathVariable Long projectId,
                                           @RequestBody PolicyView req) {
        if (req.strictness == null
                || !List.of("STRICT","NORMAL","LENIENT","OFF")
                       .contains(req.strictness.toUpperCase())) {
            throw new IllegalArgumentException("strictness 必须是 STRICT/NORMAL/LENIENT/OFF");
        }

        // thresholds JSON (允许 null 走默认)
        String tjson = null;
        if (req.thresholdHigh != null && req.thresholdMedium != null && req.thresholdLow != null) {
            tjson = String.format("{\"high\":%s,\"medium\":%s,\"low\":%s}",
                    req.thresholdHigh, req.thresholdMedium, req.thresholdLow);
        }
        String djson = null;
        if (req.deadlineHigh != null && req.deadlineMedium != null && req.deadlineLow != null) {
            djson = String.format("{\"high\":%d,\"medium\":%d,\"low\":%d}",
                    req.deadlineHigh, req.deadlineMedium, req.deadlineLow);
        }

        jdbcTemplate.update(
                "UPDATE insp_projects SET corrective_strictness=?, " +
                " corrective_severity_thresholds=?, corrective_default_deadlines=? " +
                " WHERE id=?",
                req.strictness.toUpperCase(), tjson, djson, projectId);

        return getPolicy(projectId);
    }

    // ==================== 复发警示 ====================

    /**
     * 拉取该任务每个 itemCode 在该主体过去 30 天的复发计数.
     * <p>用于检查员开始打分前/打分时的"该项历史问题"提示.
     */
    @GetMapping("/recurrence")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<List<RecurrenceView>> recurrence(
            @RequestParam Long projectId,
            @RequestParam Long subjectId) {
        if (projectId == null || subjectId == null) {
            throw new IllegalArgumentException("projectId 与 subjectId 必填");
        }

        // 单 SQL 拿所有 itemCode 复发数 — I1: 加 t.org_unit_id scope
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
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

        List<RecurrenceView> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            RecurrenceView v = new RecurrenceView();
            v.itemCode = (String) r.get("itemCode");
            v.itemName = (String) r.get("itemName");
            v.recurCount = ((Number) r.get("recurCount")).intValue();
            Object t = r.get("lastSeenAt");
            v.lastSeenAt = t == null ? null : t.toString();
            out.add(v);
        }
        return Result.success(out);
    }

    public static class RecurrenceView {
        public String itemCode;
        public String itemName;
        public Integer recurCount;
        public String lastSeenAt;
    }

    // ==================== 引擎 KPI ====================

    /**
     * 引擎 KPI 看板: 系统/人工占比 + Top10 复发 itemCode + severity 分布.
     * <p>项目级 (传 projectId) / 全局 (不传).
     */
    @GetMapping("/kpi")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<EngineKpi> kpi(@RequestParam(required = false) Long projectId) {
        EngineKpi kpi = new EngineKpi();
        // I1: org_unit_id scope 加入 pidFilter (insp_corrective_cases 有 org_unit_id 列)
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
        kpi.engineRatio = kpi.totalCount == 0 ? 0.0
                : Math.round(kpi.engineCount * 1000.0 / kpi.totalCount) / 10.0;

        // 2. severity 分布 (基于 priority 字段 — engine 写 LOW/MEDIUM/HIGH 同 priority)
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
        long closed = num(closeRow.get("closed"));
        long total = num(closeRow.get("total"));
        kpi.closeRate = total == 0 ? 0.0 : Math.round(closed * 1000.0 / total) / 10.0;

        // 4. 平均严重度分
        Double avgSev = jdbcTemplate.queryForObject(
                "SELECT AVG(severity_score) FROM insp_corrective_cases " +
                " WHERE deleted = 0 AND severity_score IS NOT NULL" + pidFilter,
                Double.class);
        kpi.avgSeverityScore = avgSev == null ? 0.0
                : Math.round(avgSev * 1000.0) / 1000.0;

        // 5. Top10 复发 itemCode (基于 submission_details 30 天)
        // I1: join t 后用 t.org_unit_id 收窄
        String pidJoin = (projectId == null ? "" : " AND t.project_id = " + projectId.longValue())
                + scopeHelper.orgScopeClause("t.org_unit_id");
        List<Map<String, Object>> top = jdbcTemplate.queryForList(
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
        kpi.topRecurring = new ArrayList<>();
        for (Map<String, Object> r : top) {
            Map<String, Object> e = new HashMap<>();
            e.put("itemCode", r.get("itemCode"));
            e.put("itemName", r.get("itemName"));
            e.put("recurCount", num(r.get("recur")));
            kpi.topRecurring.add(e);
        }
        return Result.success(kpi);
    }

    private static long num(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    public static class EngineKpi {
        public long engineCount;
        public long manualCount;
        public long totalCount;
        public double engineRatio;       // 0-100 系统建议占比 %
        public Map<String, Long> severityDist;
        public double closeRate;          // 0-100 关闭率 %
        public double avgSeverityScore;   // 0-1 平均严重度
        public List<Map<String, Object>> topRecurring;
    }

    public static class PolicyView {
        public String strictness;
        public Double thresholdHigh;
        public Double thresholdMedium;
        public Double thresholdLow;
        public Integer deadlineHigh;
        public Integer deadlineMedium;
        public Integer deadlineLow;
    }

    // ==================== 检查项级 ItemRule GET / PUT ====================

    /** 取检查项整改覆盖规则. 返回原 JSON (前端解析). */
    @GetMapping("/template-items/{itemId}/override")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<String> getItemOverride(@PathVariable Long itemId) {
        try {
            String json = jdbcTemplate.queryForObject(
                    "SELECT corrective_override FROM insp_template_items WHERE id=?",
                    String.class, itemId);
            return Result.success(json);
        } catch (Exception e) {
            return Result.success(null);
        }
    }

    /**
     * 更新检查项整改覆盖规则.
     * <p>请求体允许直接传 JSON 字符串 (overrideJson) 或结构化字段;
     * null/空 → 清除覆盖, 退回到项目级策略.
     */
    @PutMapping("/template-items/{itemId}/override")
    @CasbinAccess(resource = "insp:template", action = "update")
    public Result<String> updateItemOverride(@PathVariable Long itemId,
                                              @RequestBody(required = false) Map<String, Object> body) {
        String json = null;
        if (body != null && !body.isEmpty()) {
            try {
                json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
            } catch (Exception e) {
                throw new IllegalArgumentException("规则 JSON 序列化失败: " + e.getMessage());
            }
        }
        jdbcTemplate.update(
                "UPDATE insp_template_items SET corrective_override=? WHERE id=?",
                json, itemId);
        return Result.success(json);
    }
}
