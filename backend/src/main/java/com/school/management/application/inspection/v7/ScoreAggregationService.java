package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.domain.inspection.service.ScoreCalculationDomainService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 分数汇总服务（V62 简化版）
 * 负责：
 * 1. 按 cycleDate 维度为项目确定等级（gradeProjectScore）
 * 2. 级联重算：SubmissionDetail → Submission 总分 → Task 汇总 → ProjectScore
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ScoreAggregationService {

    private final InspSubmissionRepository submissionRepository;
    private final SubmissionDetailRepository detailRepository;
    private final InspTaskRepository taskRepository;
    private final InspProjectRepository projectRepository;
    private final ProjectScoreRepository scoreRepository;
    private final ScoringProfileRepository scoringProfileRepository;
    private final ScoreDimensionRepository dimensionRepository;
    private final CalculationRuleV7Repository ruleRepository;
    private final GradeBandRepository gradeBandRepository;
    private final TemplateSectionRepository sectionRepository;
    private final ScoreCalculationDomainService scoreCalculationService;
    private final ObjectMapper objectMapper;

    // ========== Grade Determination ==========

    /**
     * 为项目在指定周期日确定等级
     */
    @Transactional
    public ProjectScore gradeProjectScore(Long projectId, LocalDate cycleDate) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));

        ProjectScore score = scoreRepository.findByProjectIdAndCycleDate(projectId, cycleDate)
                .orElse(null);
        if (score == null) {
            log.debug("项目 {} 在 {} 无分数记录", projectId, cycleDate);
            return null;
        }

        // 确定等级（旧 GradeBand 路径，评价体系由 IndicatorScoreService 负责）
        String grade = null;
        if (score.getScore() != null && project.getScoringProfileId() != null) {
            grade = determineGrade(project.getScoringProfileId(), score.getScore());
        }

        if (grade != null) {
            score.updateScore(score.getScore(), grade, score.getTargetCount(), score.getDetail());
            scoreRepository.save(score);
        }

        return score;
    }

    private String determineGrade(Long scoringProfileId, BigDecimal score) {
        if (score == null) return null;
        List<GradeBand> bands = gradeBandRepository.findByScoringProfileId(scoringProfileId);
        for (GradeBand band : bands) {
            if (band.getMinScore() == null || band.getMaxScore() == null) continue;
            if (score.compareTo(band.getMinScore()) >= 0 && score.compareTo(band.getMaxScore()) <= 0) {
                return band.getGradeCode();
            }
        }
        return null;
    }



    // ========== Cascade Score Recalculation ==========

    /**
     * 级联重算：从某个 submission 开始，重算整条链。
     * 调用场景：修改 SubmissionDetail 分数后、申诉通过后，以及管理员手动触发。
     *
     * 链路：SubmissionDetail → Submission 总分 → Task completedTargets 计数 → ProjectScore
     *
     * @param submissionId 需要重算的 submission ID
     * @return 重算后的 submission
     */
    @Transactional
    public InspSubmission recalculateFromSubmission(Long submissionId) {
        // 1. 加载 submission 及其关联的 task / project
        InspSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + submissionId));
        Long taskId = submission.getTaskId();
        InspTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("任务不存在: " + taskId));
        Long projectId = task.getProjectId();
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalStateException("项目不存在: " + projectId));

        // 2. 读取所有明细，重新计算 submission 分数
        List<SubmissionDetail> details = detailRepository.findBySubmissionId(submissionId);
        ScoreFields fields = computeScoreFields(project, details, submission.getSectionId());

        // 3. 更新 submission 分数（recalculate 不改变状态，可在 COMPLETED 状态下调用）
        submission.recalculate(fields.baseScore, fields.finalScore,
                fields.deductionTotal, fields.bonusTotal,
                fields.scoreBreakdown, fields.grade, fields.passed);
        submission = submissionRepository.save(submission);

        // 4. 重算该 Task 的所有 Submission 汇总（completedTargets 计数）
        updateTaskCompletedCount(task);

        // 5. 找到该 Task 所属的 Project，重算该 Project 在该日期的 ProjectScore
        recomputeProjectScore(project, task.getTaskDate());

        log.info("级联重算完成: submissionId={}, taskId={}, projectId={}, date={}",
                submissionId, task.getId(), project.getId(), task.getTaskDate());

        return submission;
    }

    /**
     * 计算 submission 的分数字段（与 completeSubmission 逻辑共享）
     */
    ScoreFields computeScoreFields(InspProject project, List<SubmissionDetail> details, Long sectionId) {
        Long profileId = project.getScoringProfileId();
        if (profileId != null) {
            ScoringProfile profile = scoringProfileRepository.findById(profileId)
                    .orElseThrow(() -> new IllegalStateException("评分配置不存在: " + profileId));
            List<ScoreDimension> dimensions = dimensionRepository.findByScoringProfileId(profileId);
            List<CalculationRuleV7> rules = ruleRepository.findByScoringProfileIdOrderByPriority(profileId);
            List<GradeBand> gradeBands = gradeBandRepository.findByScoringProfileId(profileId);

            List<ScoreCalculationDomainService.ItemScoreInput> inputs = buildItemScoreInputs(details);
            ScoreCalculationDomainService.ScoreResult result = scoreCalculationService.calculate(
                    profile, dimensions, rules, gradeBands, inputs, 0);

            String breakdown = serializeBreakdown(result);
            return new ScoreFields(
                    result.getRawScore(), result.getFinalScore(),
                    result.getDeductionTotal(), result.getBonusTotal(),
                    breakdown, result.getGrade(), result.isPassed());
        } else {
            // 无评分配置：简单汇总 + 从分区 scoringConfig 读取 gradeBands 计算等级
            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal maxPossible = BigDecimal.ZERO;
            for (SubmissionDetail d : details) {
                if (d.getScore() != null) {
                    sum = sum.add(d.getScore());
                }
                // 从每个 detail 的 scoringConfig 读取 maxScore
                maxPossible = maxPossible.add(parseMaxScore(d.getScoringConfig()));
            }

            String grade = null;
            Boolean passed = null;

            if (sectionId != null) {
                // 分区级 ScoringProfile + GradeBand（等级评价由 IndicatorScoreService 负责）
                try {
                    ScoringProfile sectionProfile = scoringProfileRepository.findBySectionId(sectionId).orElse(null);
                    if (sectionProfile != null) {
                        List<GradeBand> bands = gradeBandRepository.findByScoringProfileId(sectionProfile.getId());
                        if (!bands.isEmpty()) {
                            for (GradeBand band : bands) {
                                if (band.getMinScore() == null || band.getMaxScore() == null) continue;
                                if (sum.compareTo(band.getMinScore()) >= 0 && sum.compareTo(band.getMaxScore()) <= 0) {
                                    // 用 gradeName（自定义标签如"优秀"），无则用 gradeCode
                                    grade = band.getGradeName() != null ? band.getGradeName() : band.getGradeCode();
                                    break;
                                }
                            }
                            if (grade != null && maxPossible.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal pct = sum.multiply(BigDecimal.valueOf(100))
                                        .divide(maxPossible, 2, RoundingMode.HALF_UP);
                                passed = pct.compareTo(BigDecimal.valueOf(60)) >= 0;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("从分区 {} 的 ScoringProfile 读取 GradeBand 失败: {}", sectionId, e.getMessage());
                }

                // 回退：从分区 scoringConfig JSON 读取 gradeBands
                if (grade == null) {
                    try {
                        TemplateSection section = sectionRepository.findById(sectionId).orElse(null);
                        if (section != null && section.getScoringConfig() != null
                                && !section.getScoringConfig().isBlank()) {
                            JsonNode configNode = objectMapper.readTree(section.getScoringConfig());
                            JsonNode gradeBandsNode = configNode.get("gradeBands");
                            if (gradeBandsNode != null && gradeBandsNode.isArray() && gradeBandsNode.size() > 0
                                    && maxPossible.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal percentage = sum.multiply(BigDecimal.valueOf(100))
                                        .divide(maxPossible, 2, RoundingMode.HALF_UP);
                                for (JsonNode band : gradeBandsNode) {
                                    BigDecimal minPct = band.has("minPercent")
                                            ? new BigDecimal(band.get("minPercent").asText()) : BigDecimal.ZERO;
                                    BigDecimal maxPct = band.has("maxPercent")
                                            ? new BigDecimal(band.get("maxPercent").asText()) : BigDecimal.valueOf(100);
                                    if (percentage.compareTo(minPct) >= 0 && percentage.compareTo(maxPct) <= 0) {
                                        grade = band.has("label") ? band.get("label").asText()
                                                : (band.has("grade") ? band.get("grade").asText() : null);
                                        break;
                                    }
                                }
                                if (grade != null) {
                                    passed = percentage.compareTo(BigDecimal.valueOf(60)) >= 0;
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("从分区 {} 解析 scoringConfig gradeBands 失败: {}", sectionId, e.getMessage());
                    }
                }
            }

            return new ScoreFields(sum, sum, BigDecimal.ZERO, BigDecimal.ZERO, null, grade, passed);
        }
    }

    /**
     * 从 detail 的 scoringConfig JSON 解析 maxScore 值
     */
    private BigDecimal parseMaxScore(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) return BigDecimal.ZERO;
        try {
            var node = objectMapper.readTree(scoringConfig);
            // 1. maxScore > 0
            if (node.has("maxScore")) {
                BigDecimal ms = new BigDecimal(node.get("maxScore").asText());
                if (ms.compareTo(BigDecimal.ZERO) > 0) return ms;
            }
            // 2. CUMULATIVE: baseScore as max
            if (node.has("baseScore")) {
                BigDecimal bs = new BigDecimal(node.get("baseScore").asText());
                if (bs.compareTo(BigDecimal.ZERO) > 0) return bs;
            }
            // 3. SCORE_TABLE: highest score in table
            if (node.has("table") && node.get("table").isArray()) {
                BigDecimal maxVal = BigDecimal.ZERO;
                for (var entry : node.get("table")) {
                    if (entry.has("score")) {
                        BigDecimal s = new BigDecimal(entry.get("score").asText());
                        if (s.compareTo(maxVal) > 0) maxVal = s;
                    }
                }
                if (maxVal.compareTo(BigDecimal.ZERO) > 0) return maxVal;
            }
            // 4. Fallback: passScore
            if (node.has("passScore")) {
                return new BigDecimal(node.get("passScore").asText());
            }
        } catch (Exception e) {
            log.debug("解析 scoringConfig maxScore 失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 公开方法：按 projectId + cycleDate 重算 ProjectScore
     * 调用场景：任务发布后触发分数重算
     */
    @Transactional
    public void recomputeProjectScore(Long projectId, LocalDate cycleDate) {
        InspProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        recomputeProjectScore(project, cycleDate);
    }

    /** 重算 ProjectScore：所有已完成 submissions 的平均分 */
    private void recomputeProjectScore(InspProject project, LocalDate cycleDate) {
        List<InspTask> dateTasks = taskRepository.findByProjectIdAndTaskDate(project.getId(), cycleDate);
        if (dateTasks.isEmpty()) return;

        BigDecimal totalScore = BigDecimal.ZERO;
        int count = 0;

        for (InspTask t : dateTasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(t.getId());
            for (InspSubmission sub : submissions) {
                if (sub.getStatus() == SubmissionStatus.COMPLETED && sub.getFinalScore() != null) {
                    totalScore = totalScore.add(sub.getFinalScore());
                    count++;
                }
            }
        }

        if (count == 0) {
            log.debug("项目 {} 日期 {} 无已完成提交，跳过 ProjectScore 重算",
                    project.getProjectCode(), cycleDate);
            return;
        }

        BigDecimal avgScore = totalScore.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        ProjectScore ps = scoreRepository.findByProjectIdAndCycleDate(project.getId(), cycleDate)
                .orElse(ProjectScore.create(project.getId(), cycleDate));
        // 保留现有等级，仅更新分数和计数
        ps.updateScore(avgScore, ps.getGrade(), count, ps.getDetail());
        scoreRepository.save(ps);

        log.info("ProjectScore 重算完成: project={}, date={}, avgScore={}, count={}",
                project.getProjectCode(), cycleDate, avgScore, count);
    }

    /** 更新 task 的 completedTargets / skippedTargets 计数（按不重复 targetId 统计） */
    private void updateTaskCompletedCount(InspTask task) {
        List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
        // 按 targetId 去重统计，一个目标有多个分区 submission 只算一个
        long distinctTargets = submissions.stream()
                .map(InspSubmission::getTargetId).distinct().count();
        // 一个目标的所有 submission 都 COMPLETED 才算该目标完成
        var byTarget = submissions.stream().collect(java.util.stream.Collectors.groupingBy(InspSubmission::getTargetId));
        int completed = 0;
        int skipped = 0;
        for (var entry : byTarget.entrySet()) {
            var subs = entry.getValue();
            boolean allDone = subs.stream().allMatch(s ->
                    s.getStatus() == SubmissionStatus.COMPLETED || s.getStatus() == SubmissionStatus.SKIPPED);
            boolean anySkipped = subs.stream().anyMatch(s -> s.getStatus() == SubmissionStatus.SKIPPED);
            if (allDone) {
                if (anySkipped) skipped++;
                else completed++;
            }
        }
        task.updateTargetCounts((int) distinctTargets, completed, skipped);
        taskRepository.save(task);
    }

    private List<ScoreCalculationDomainService.ItemScoreInput> buildItemScoreInputs(
            List<SubmissionDetail> details) {
        List<ScoreCalculationDomainService.ItemScoreInput> inputs = new ArrayList<>();
        for (SubmissionDetail detail : details) {
            String scoringMode = mapScoringMode(detail.getScoringMode());
            BigDecimal configScore = parseConfigScore(detail.getScoringConfig());
            BigDecimal responseNumericValue = parseNumericValue(detail.getResponseValue());
            int quantity = parseQuantity(detail.getResponseValue(), detail.getScoringMode());
            Long dimensionId = parseDimensionId(detail.getDimensions());

            inputs.add(new ScoreCalculationDomainService.ItemScoreInput(
                    detail.getItemCode(), dimensionId, scoringMode,
                    configScore, responseNumericValue, quantity, null));
        }
        return inputs;
    }

    private String mapScoringMode(ScoringMode mode) {
        if (mode == null) return "DEDUCTION";
        switch (mode) {
            case DEDUCTION:
            case TIERED_DEDUCTION:
                return "DEDUCTION";
            case ADDITION:
            case CUMULATIVE:
                return "ADDITION";
            case DIRECT:
                return "FIXED";
            default:
                return "RESPONSE_MAPPED";
        }
    }

    private BigDecimal parseConfigScore(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) return BigDecimal.ZERO;
        try {
            var node = objectMapper.readTree(scoringConfig);
            if (node.has("score")) return new BigDecimal(node.get("score").asText());
            if (node.has("configScore")) return new BigDecimal(node.get("configScore").asText());
            if (node.has("baseScore")) return new BigDecimal(node.get("baseScore").asText());
        } catch (Exception e) {
            log.debug("解析 scoringConfig 分值失败: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal parseNumericValue(String responseValue) {
        if (responseValue == null || responseValue.isBlank()) return null;
        try {
            return new BigDecimal(responseValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int parseQuantity(String responseValue, ScoringMode mode) {
        if (mode == ScoringMode.DEDUCTION || mode == ScoringMode.ADDITION
                || mode == ScoringMode.CUMULATIVE || mode == ScoringMode.TIERED_DEDUCTION) {
            BigDecimal val = parseNumericValue(responseValue);
            if (val != null) {
                return Math.max(val.intValue(), 0);
            }
        }
        return 1;
    }

    private Long parseDimensionId(String dimensions) {
        if (dimensions == null || dimensions.isBlank()) return null;
        try {
            var node = objectMapper.readTree(dimensions);
            if (node.isArray() && node.size() > 0) {
                return node.get(0).asLong();
            }
            if (node.has("dimensionId")) {
                return node.get("dimensionId").asLong();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private String serializeBreakdown(ScoreCalculationDomainService.ScoreResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("序列化评分结果失败", e);
            return null;
        }
    }

    /** 评分字段值对象（package-private，供 InspSubmissionApplicationService 共享） */
    static class ScoreFields {
        final BigDecimal baseScore;
        final BigDecimal finalScore;
        final BigDecimal deductionTotal;
        final BigDecimal bonusTotal;
        final String scoreBreakdown;
        final String grade;
        final Boolean passed;

        ScoreFields(BigDecimal baseScore, BigDecimal finalScore,
                    BigDecimal deductionTotal, BigDecimal bonusTotal,
                    String scoreBreakdown, String grade, Boolean passed) {
            this.baseScore = baseScore;
            this.finalScore = finalScore;
            this.deductionTotal = deductionTotal;
            this.bonusTotal = bonusTotal;
            this.scoreBreakdown = scoreBreakdown;
            this.grade = grade;
            this.passed = passed;
        }
    }
}
