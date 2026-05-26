package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.CorrectiveCaseApplicationService;
import com.school.management.application.inspection.CorrectiveSuggestionApplicationService;
import com.school.management.application.inspection.CorrectiveSuggestionService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.correction.CorrectionEngine;
import com.school.management.domain.inspection.correction.CorrectionVerdict;
import com.school.management.domain.inspection.correction.ItemRule;
import com.school.management.domain.inspection.correction.ProjectCorrectivePolicy;
import com.school.management.domain.inspection.correction.Severity;
import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.corrective.CasePriority;
import com.school.management.domain.inspection.model.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final CorrectiveSuggestionApplicationService suggestionAppService;
    private final CorrectionEngine correctionEngine;
    /** 复用单例 — 不要每请求 new ObjectMapper() (开销大). */
    private final ObjectMapper objectMapper;

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
    @Transactional
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

            String caseCode = "CC-" + IdWorker.getId();
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
                    SecurityUtils.requireCurrentUserId());

            // 写入 V110 引擎字段 (suggested_by_system / severity_score / explain_trace / suggestion_reason)
            suggestionAppService.writeEngineFields(
                    saved.getId(),
                    truncate(v.getReason(), 500),
                    v.getSeverityScore(),
                    suggestionService.serializeTrace(v));

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
        // 注: 字段必填校验由 confirm() 方法内显式 if 抛 IllegalArgumentException 处理
        //     (public 字段类无 getter, Bean Validation 仍可标注但此处沿用既有显式校验)
    }

    // ==================== 项目策略 GET / PUT ====================

    /** 取项目整改策略. V20260524_7: enabled / strictnessAdjustment / autoCreateLevel 三段式. */
    @GetMapping("/projects/{projectId}/policy")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<PolicyView> getPolicy(@PathVariable Long projectId) {
        var p = suggestionService.loadPolicy(projectId);
        PolicyView v = new PolicyView();
        v.enabled = p.enabled();
        v.strictnessAdjustment = p.strictnessAdjustment();
        v.autoCreateLevel = p.autoCreateLevel() == null ? "NONE" : p.autoCreateLevel().name();
        // 旧 strictness 字段保留, 由新字段反推: !enabled→OFF, adj>=1→STRICT, adj<=-1→LENIENT, 其他→NORMAL
        if (!p.enabled()) v.strictness = "OFF";
        else if (p.strictnessAdjustment() >= 1) v.strictness = "STRICT";
        else if (p.strictnessAdjustment() <= -1) v.strictness = "LENIENT";
        else v.strictness = "NORMAL";
        v.thresholdHigh = p.thresholds().high();
        v.thresholdMedium = p.thresholds().medium();
        v.thresholdLow = p.thresholds().low();
        v.deadlineHigh = p.deadlines().high();
        v.deadlineMedium = p.deadlines().medium();
        v.deadlineLow = p.deadlines().low();
        return Result.success(v);
    }

    /**
     * 更新项目整改策略 (V20260524_7 三段式).
     * <p>请求体:
     * <ul>
     *   <li>enabled — 总开关</li>
     *   <li>strictnessAdjustment — 调档 -2..+2</li>
     *   <li>autoCreateLevel — HIGH/MEDIUM/LOW/NONE</li>
     *   <li>thresholds/deadlines — 可选阈值与时限</li>
     * </ul>
     * <p>兼容: 老前端仍可传 strictness, 映射为新字段 (STRICT→adj=+1/auto=LOW, LENIENT→adj=-1/auto=NONE, OFF→enabled=false).
     */
    @PutMapping("/projects/{projectId}/policy")
    @CasbinAccess(resource = "insp:project", action = "update")
    public Result<PolicyView> updatePolicy(@PathVariable Long projectId,
                                           @RequestBody PolicyView req) {
        Boolean enabled = req.enabled;
        Integer adj = req.strictnessAdjustment;
        String autoLevel = req.autoCreateLevel;

        // 兼容: 旧 strictness → 新字段
        if (enabled == null && adj == null && autoLevel == null && req.strictness != null) {
            switch (req.strictness.toUpperCase()) {
                case "STRICT":  enabled = true;  adj = 1;  autoLevel = "LOW";    break;
                case "NORMAL":  enabled = true;  adj = 0;  autoLevel = "NONE";   break;
                case "LENIENT": enabled = true;  adj = -1; autoLevel = "NONE";   break;
                case "OFF":     enabled = false; adj = 0;  autoLevel = "NONE";   break;
                default: throw new IllegalArgumentException(
                        "strictness 必须是 STRICT/NORMAL/LENIENT/OFF");
            }
        }
        if (enabled == null) enabled = true;
        if (adj == null) adj = 0;
        if (adj < -2 || adj > 2) {
            throw new IllegalArgumentException("strictnessAdjustment 必须在 -2..+2 之间");
        }
        if (autoLevel == null) autoLevel = "NONE";
        if (!List.of("HIGH","MEDIUM","LOW","NONE").contains(autoLevel.toUpperCase())) {
            throw new IllegalArgumentException("autoCreateLevel 必须是 HIGH/MEDIUM/LOW/NONE");
        }

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

        suggestionAppService.updateProjectPolicy(
                projectId, enabled, adj, autoLevel.toUpperCase(), tjson, djson);

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
        List<Map<String, Object>> rows = suggestionAppService.queryRecurrence(projectId, subjectId);

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
        CorrectiveSuggestionApplicationService.EngineKpiData data =
                suggestionAppService.queryEngineKpi(projectId);

        EngineKpi kpi = new EngineKpi();
        kpi.engineCount = data.engineCount;
        kpi.manualCount = data.manualCount;
        kpi.totalCount = data.totalCount;
        kpi.engineRatio = kpi.totalCount == 0 ? 0.0
                : Math.round(kpi.engineCount * 1000.0 / kpi.totalCount) / 10.0;

        kpi.severityDist = data.severityDist;

        kpi.closeRate = data.closeTotalCount == 0 ? 0.0
                : Math.round(data.closedCount * 1000.0 / data.closeTotalCount) / 10.0;

        kpi.avgSeverityScore = data.avgSeverityScore == null ? 0.0
                : Math.round(data.avgSeverityScore * 1000.0) / 1000.0;

        kpi.topRecurring = new ArrayList<>();
        for (Map<String, Object> r : data.topRecurring) {
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
        /** @deprecated 旧 strictness 字段, V20260524_7 后由 enabled+adjustment+autoLevel 替代. 仅保留兼容. */
        @Deprecated
        public String strictness;
        // V20260524_7 新字段
        public Boolean enabled;
        public Integer strictnessAdjustment;
        public String autoCreateLevel;        // HIGH / MEDIUM / LOW / NONE

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
        return Result.success(suggestionAppService.getItemOverride(itemId));
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
                json = objectMapper.writeValueAsString(body);
            } catch (Exception e) {
                throw new IllegalArgumentException("规则 JSON 序列化失败: " + e.getMessage());
            }
        }
        suggestionAppService.updateItemOverride(itemId, json);
        return Result.success(json);
    }

    // ==================== Simulate API (WhatIf 模拟器) ====================

    /**
     * 模拟整改判定 — 不需要真实 submission, 配置阶段即可验证规则.
     * <p>典型用法: 前端 RuleSimulator 在用户配置整改规则时, 让其输入"假设响应",
     * 引擎计算 verdict 实时反馈, 消除"配完不知道对不对"的认知 gap.
     */
    @PostMapping("/simulate")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<SimulateView> simulate(@RequestBody SimulateRequest req) {
        if (req == null || req.scoringMode == null) {
            throw new IllegalArgumentException("scoringMode 必填");
        }
        ScoringMode mode;
        try { mode = ScoringMode.valueOf(req.scoringMode.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("scoringMode 非法"); }

        ProjectCorrectivePolicy policy = req.projectId != null
                ? suggestionService.loadPolicy(req.projectId)
                : ProjectCorrectivePolicy.normalDefault();

        ItemRule itemRule = ItemRule.fromJson(req.itemRuleJson);

        java.math.BigDecimal score = req.score == null ? null : java.math.BigDecimal.valueOf(req.score);
        java.math.BigDecimal weight = req.itemWeight == null ? null : java.math.BigDecimal.valueOf(req.itemWeight);

        com.school.management.domain.inspection.model.execution.SubmissionDetail detail =
                com.school.management.domain.inspection.model.execution.SubmissionDetail.builder()
                .id(-1L)
                .itemCode(req.itemCode != null ? req.itemCode : "SIM")
                .itemName(req.itemName != null ? req.itemName : "模拟题")
                .scoringMode(mode)
                .responseValue(req.responseValue)
                .score(score)
                .itemWeight(weight)
                .scoringConfig(req.scoringConfigJson)
                .build();

        CorrectionVerdict v = correctionEngine.judge(detail, policy, itemRule, req.recurrenceCount == null ? 0 : req.recurrenceCount);

        SimulateView out = new SimulateView();
        out.severity = v.getSeverity().name();
        out.severityScore = v.getSeverityScore();
        out.mustCorrect = v.isMustCorrect();
        out.deadlineDays = v.getSuggestedDeadlineDays();
        out.reason = v.getReason();
        out.trace = v.getTrace() == null ? List.of()
                : v.getTrace().stream().map(t -> {
                    SimulateTrace st = new SimulateTrace();
                    st.layer = t.layer();
                    st.rule = t.rule();
                    st.input = t.input();
                    st.output = t.output();
                    return st;
                }).collect(java.util.stream.Collectors.toList());
        return Result.success(out);
    }

    public static class SimulateRequest {
        public Long projectId;          // 可选, 用于加载项目策略
        public String scoringMode;      // PASS_FAIL / RATING_SCALE / ...
        public String responseValue;    // 检查员假设填的响应值
        public Double score;            // 假设得分 (continuous 模式)
        public Double itemWeight;       // 题权重 (扣分模式用)
        public String scoringConfigJson; // 题目评分配置 (RISK_MATRIX/THRESHOLD 等需要)
        public String itemRuleJson;     // 题目整改规则 JSON (草稿态, 未保存)
        public Integer recurrenceCount; // 假设近 30 天复发次数
        public String itemCode;
        public String itemName;
    }

    public static class SimulateView {
        public String severity;         // HIGH / MEDIUM / LOW / NONE
        public double severityScore;    // sev 0~1
        public boolean mustCorrect;     // 是否自动建单
        public int deadlineDays;        // 建议时限
        public String reason;
        public List<SimulateTrace> trace;
    }

    public static class SimulateTrace {
        public String layer;            // 决策层 (policy/itemRule/normalize/threshold/...)
        public String rule;
        public String input;
        public String output;
    }
}
