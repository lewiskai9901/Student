package com.school.management.application.inspection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.correction.CorrectionEngine;
import com.school.management.domain.inspection.correction.CorrectionVerdict;
import com.school.management.domain.inspection.correction.DeadlinePresets;
import com.school.management.domain.inspection.correction.ItemRule;
import com.school.management.domain.inspection.correction.ProjectCorrectivePolicy;
import com.school.management.domain.inspection.correction.Severity;
import com.school.management.domain.inspection.correction.SeverityThresholds;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 整改建议服务 — 包装 CorrectionEngine, 负责加载策略 + 复发计数, 输出 verdict 列表.
 *
 * <p>调用方:
 * <ul>
 *   <li>提交完成事件 → 计算 candidates 给前端确认</li>
 *   <li>前端 GET /submissions/{id}/corrective-candidates 主动拉取</li>
 *   <li>STRICT 模式下 CorrectiveAutoCreationHandler 仍直接落库</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorrectiveSuggestionService {

    private final CorrectionEngine engine;
    private final SubmissionDetailRepository detailRepository;
    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 计算指定提交的整改候选列表. */
    public List<CorrectionVerdict> suggestForSubmission(Long submissionId) {
        InspSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) return List.of();

        Long taskId = submission.getTaskId();
        Long projectId = null;
        if (taskId != null) {
            projectId = taskRepository.findById(taskId).map(InspTask::getProjectId).orElse(null);
        }

        ProjectCorrectivePolicy policy = loadPolicy(projectId);

        List<SubmissionDetail> details = detailRepository.findBySubmissionId(submissionId);
        if (details.isEmpty()) return List.of();

        Long subjectOrgId = submission.getTargetId();   // 用作复发查询 subject
        Long pid = projectId;

        return engine.judgeAll(details, policy,
                d -> countRecentRecurrence(pid, subjectOrgId, d.getItemCode()),
                d -> loadItemRule(pid, d))
                .stream()
                .filter(CorrectionVerdict::shouldSuggest)
                .toList();
    }

    /**
     * 加载题目整改规则 (架构 E 优先级链):
     * <ol>
     *   <li>项目题目个例 (insp_project_item_overrides by projectId + templateItemId)</li>
     *   <li>项目按题型规则 (insp_project_corrective_rules by projectId + scoringMode)</li>
     *   <li>EMPTY 兜底 (引擎按现有逻辑 fall through 到项目阈值)</li>
     * </ol>
     *
     * <p>注: 老的 insp_template_items.corrective_override 字段不再被引擎直接读取,
     * 该字段保留作"创建项目时的预设建议" (UI 一键导入).
     */
    public ItemRule loadItemRule(Long projectId, SubmissionDetail detail) {
        if (detail == null) return ItemRule.EMPTY;

        // 优先级 1: 项目题目个例覆盖
        if (projectId != null && detail.getTemplateItemId() != null) {
            try {
                String json = jdbcTemplate.queryForObject(
                        "SELECT rule_json FROM insp_project_item_overrides " +
                        " WHERE project_id=? AND template_item_id=? AND deleted=0",
                        String.class, projectId, detail.getTemplateItemId());
                if (json != null && !json.isBlank()) return ItemRule.fromJson(json);
            } catch (Exception ignored) { /* not found, continue */ }
        }

        // 优先级 2: 项目按题型规则
        if (projectId != null && detail.getScoringMode() != null) {
            try {
                String json = jdbcTemplate.queryForObject(
                        "SELECT rule_json FROM insp_project_corrective_rules " +
                        " WHERE project_id=? AND scoring_mode=? AND deleted=0",
                        String.class, projectId, detail.getScoringMode().name());
                if (json != null && !json.isBlank()) return ItemRule.fromJson(json);
            } catch (Exception ignored) { /* not found, continue */ }
        }

        // 优先级 3: 兜底 EMPTY (引擎 fall through 到项目阈值)
        return ItemRule.EMPTY;
    }

    /** @deprecated 兼容入口, 内部调用新链路. */
    @Deprecated
    public ItemRule loadItemRule(SubmissionDetail detail) {
        return loadItemRule(null, detail);
    }

    /**
     * 项目级策略加载 (V20260524_7 重构):
     * 新模型 enabled / strictnessAdjustment / autoCreateLevel 优先, 旧 strictness 作为回退.
     */
    public ProjectCorrectivePolicy loadPolicy(Long projectId) {
        if (projectId == null) return ProjectCorrectivePolicy.normalDefault();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT corrective_strictness, corrective_severity_thresholds, " +
                    "       corrective_default_deadlines, " +
                    "       corrective_enabled, corrective_strictness_adj, corrective_auto_create_level " +
                    "  FROM insp_projects WHERE id=?", projectId);

            // 新字段优先
            Object enabledObj = row.get("corrective_enabled");
            boolean enabled = enabledObj == null
                    ? true
                    : ((Number) enabledObj).intValue() != 0;
            int adj = row.get("corrective_strictness_adj") != null
                    ? ((Number) row.get("corrective_strictness_adj")).intValue()
                    : 0;
            Severity autoLevel = Severity.NONE;
            Object autoLevelObj = row.get("corrective_auto_create_level");
            if (autoLevelObj instanceof String s && !s.isBlank()) {
                try { autoLevel = Severity.valueOf(s.toUpperCase()); }
                catch (IllegalArgumentException ignored) {}
            }

            // 旧 strictness 兼容: 如果 enabled=0 强制视为 disabled
            // 旧 thresholds JSON 仍解析用作 sev classify 的 thresholds
            String strictness = (String) row.get("corrective_strictness");
            if (strictness == null) strictness = "NORMAL";

            SeverityThresholds t = SeverityThresholds.fromStrictness(strictness);
            String tjson = (String) row.get("corrective_severity_thresholds");
            if (tjson != null && !tjson.isBlank()) {
                try {
                    Map m = objectMapper.readValue(tjson, Map.class);
                    t = new SeverityThresholds(
                            num(m.get("high"), t.high()),
                            num(m.get("medium"), t.medium()),
                            num(m.get("low"), t.low()));
                } catch (JsonProcessingException ignored) {}
            }

            DeadlinePresets d = DeadlinePresets.DEFAULT;
            String djson = (String) row.get("corrective_default_deadlines");
            if (djson != null && !djson.isBlank()) {
                try {
                    Map m = objectMapper.readValue(djson, Map.class);
                    d = new DeadlinePresets(
                            (int) num(m.get("high"), d.high()),
                            (int) num(m.get("medium"), d.medium()),
                            (int) num(m.get("low"), d.low()));
                } catch (JsonProcessingException ignored) {}
            }

            return new ProjectCorrectivePolicy(enabled, adj, autoLevel, t, d);
        } catch (Exception e) {
            log.warn("loadPolicy({}) failed: {}, fallback to NORMAL", projectId, e.getMessage());
            return ProjectCorrectivePolicy.normalDefault();
        }
    }

    /**
     * 近 30 天同 itemCode + 同主体的"问题次数".
     *
     * <p>按 submission_details 维度统计 (而非 corrective_cases) — 修复 OFF 模式
     * 不建 case 导致复发数永远 0 的硬伤.
     *
     * <p>"问题"判定:
     * <ul>
     *   <li>response_value = FAIL / D / 1 (RATING) → 计入</li>
     *   <li>score 为负 (扣分) → 计入</li>
     *   <li>score 为正但 < weight×0.5 (得分严重偏低) → 计入</li>
     *   <li>is_flagged=1 → 计入</li>
     * </ul>
     * 按 (submission_id, item_code) 去重避免同一次提交被重复计算.
     */
    public int countRecentRecurrence(Long projectId, Long subjectOrgId, String itemCode) {
        if (projectId == null || subjectOrgId == null || itemCode == null) return 0;
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT d.submission_id) " +
                    "  FROM insp_submission_details d " +
                    "  JOIN insp_submissions s ON d.submission_id = s.id " +
                    "  JOIN insp_tasks t ON s.task_id = t.id " +
                    " WHERE t.project_id = ? " +
                    "   AND s.target_id = ? " +
                    "   AND d.item_code = ? " +
                    "   AND d.deleted = 0 " +
                    "   AND s.deleted = 0 " +
                    "   AND s.created_at >= NOW() - INTERVAL 30 DAY " +
                    "   AND ( " +
                    "        UPPER(d.response_value) IN ('FAIL','D','NO','FALSE','0') " +
                    "        OR d.score < 0 " +
                    "        OR (d.score IS NOT NULL AND d.item_weight > 0 " +
                    "            AND d.score / d.item_weight < 0.5) " +
                    "        OR d.is_flagged = 1 " +
                    "   )",
                    Integer.class, projectId, subjectOrgId, itemCode);
            return n == null ? 0 : n;
        } catch (Exception e) {
            log.debug("countRecentRecurrence failed: {}", e.getMessage());
            return 0;
        }
    }

    /** 把 verdicts 序列化为 explain_trace_json. */
    public String serializeTrace(CorrectionVerdict v) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("severity", v.getSeverity().name());
            data.put("severityScore", v.getSeverityScore());
            data.put("reason", v.getReason());
            data.put("trace", v.getTrace());
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static double num(Object o, double dflt) {
        if (o == null) return dflt;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); }
        catch (Exception e) { return dflt; }
    }
}
