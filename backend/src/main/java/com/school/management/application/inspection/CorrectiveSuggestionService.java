package com.school.management.application.inspection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.correction.CorrectionEngine;
import com.school.management.domain.inspection.correction.CorrectionVerdict;
import com.school.management.domain.inspection.correction.DeadlinePresets;
import com.school.management.domain.inspection.correction.ItemRule;
import com.school.management.domain.inspection.correction.ProjectCorrectivePolicy;
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
                this::loadItemRule)
                .stream()
                .filter(CorrectionVerdict::shouldSuggest)
                .toList();
    }

    /** 加载 insp_template_items.corrective_override → ItemRule. */
    public ItemRule loadItemRule(SubmissionDetail detail) {
        if (detail == null || detail.getTemplateItemId() == null) return ItemRule.EMPTY;
        try {
            String json = jdbcTemplate.queryForObject(
                    "SELECT corrective_override FROM insp_template_items WHERE id=?",
                    String.class, detail.getTemplateItemId());
            return ItemRule.fromJson(json);
        } catch (Exception e) {
            return ItemRule.EMPTY;
        }
    }

    /** 项目级策略加载: insp_projects.corrective_* 字段. */
    public ProjectCorrectivePolicy loadPolicy(Long projectId) {
        if (projectId == null) return ProjectCorrectivePolicy.normalDefault();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT corrective_strictness, corrective_severity_thresholds, " +
                    "       corrective_default_deadlines " +
                    "  FROM insp_projects WHERE id=?", projectId);
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

            return new ProjectCorrectivePolicy(strictness, t, d);
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
