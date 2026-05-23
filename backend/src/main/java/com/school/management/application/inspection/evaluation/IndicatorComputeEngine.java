package com.school.management.application.inspection.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.scoring.GradeScheme;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.MissingPolicy;
import com.school.management.domain.inspection.model.scoring.RankDirection;
import com.school.management.domain.inspection.model.scoring.SubmissionDateField;
import com.school.management.domain.inspection.repository.GradeDefinitionRepository;
import com.school.management.domain.inspection.repository.GradeSchemeRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * 评级计算引擎 (Phase 3, 2026-05-23).
 *
 * <p>责任纯粹: 给定 indicator + period 边界, 算出 DRAFT 状态的 {@link IndicatorResult} 列表,
 * <strong>不</strong>访问 IndicatorResultRepository, <strong>不</strong>负责持久化/状态机.
 * 持久化与 supersede 链由 {@link IndicatorEvaluationService} 编排.
 *
 * <p>计算步骤:
 * <ol>
 *   <li>按 {@code sourceSectionIds} + 周期 + {@code submissionDateField} 收集 submission</li>
 *   <li>按 target 分组, 应用 {@code missingPolicy} 处理无数据 section</li>
 *   <li>多 section 时按 {@code weightsBySection} 加权 (默认等权)</li>
 *   <li>按 {@code rankDirection} 跨所有 target 排名</li>
 *   <li>按 {@code gradeScheme} 挂等级</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorComputeEngine {

    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final GradeSchemeRepository gradeSchemeRepository;
    private final GradeDefinitionRepository gradeDefinitionRepository;

    /**
     * 主入口: 算出 DRAFT 结果列表. 同 indicator 下每个 target 一条.
     */
    public List<IndicatorResult> compute(Indicator indicator,
                                         String periodKey,
                                         LocalDate periodStart,
                                         LocalDate periodEnd) {
        if (indicator == null) throw new IllegalArgumentException("indicator 必填");
        if (periodKey == null || periodKey.isBlank()) throw new IllegalArgumentException("periodKey 必填");
        if (periodStart == null || periodEnd == null) throw new IllegalArgumentException("周期边界必填");
        if (periodStart.isAfter(periodEnd)) throw new IllegalArgumentException("periodStart > periodEnd");

        List<Long> sectionIds = indicator.getSourceSectionIds();
        if (sectionIds == null || sectionIds.isEmpty()) {
            log.warn("Indicator {} 无 sourceSectionIds, 跳过", indicator.getId());
            return List.of();
        }

        // 1. 收集 period 内所有 submission, 按 (target → section → score) 索引
        CollectionResult coll = collectSubmissions(indicator, sectionIds, periodStart, periodEnd);
        Map<TargetRef, Map<Long, List<BigDecimal>>> bySectionByTarget = coll.byTarget;
        Map<TargetRef, Set<Long>> submissionIdsByTarget = coll.subIdsByTarget;
        Map<TargetRef, String> namesByTarget = coll.namesByTarget;

        // 2. WAIT 全局判断: 若任一 target 任一 section 无数据, 直接返回空 (推迟)
        if (indicator.getMissingPolicy() == MissingPolicy.WAIT
                && hasAnyMissingSection(bySectionByTarget, sectionIds)) {
            log.info("Indicator {} missingPolicy=WAIT, 有空 section, 推迟评估", indicator.getId());
            return List.of();
        }

        // 3. 对每个 target 算聚合分
        Map<TargetRef, BigDecimal> targetScores = new LinkedHashMap<>();
        Map<TargetRef, List<Long>> targetSubmissionIds = new LinkedHashMap<>();
        for (Map.Entry<TargetRef, Map<Long, List<BigDecimal>>> e : bySectionByTarget.entrySet()) {
            TargetRef target = e.getKey();
            Map<Long, List<BigDecimal>> bySection = e.getValue();

            BigDecimal score = aggregateAcrossSections(indicator, sectionIds, bySection);
            if (score == null) continue; // missingPolicy 跳过

            targetScores.put(target, score);
            Set<Long> ids = submissionIdsByTarget.getOrDefault(target, Set.of());
            targetSubmissionIds.put(target, new ArrayList<>(ids));
        }

        if (targetScores.isEmpty()) return List.of();

        // 4. 排名
        Map<TargetRef, Integer> rankMap = computeRanks(targetScores, indicator.getRankDirection());

        // 5. 等级
        GradeScheme scheme = loadScheme(indicator.getGradeSchemeId());
        List<BigDecimal> allValues = new ArrayList<>(targetScores.values());

        // 6. 组装结果
        List<IndicatorResult> results = new ArrayList<>(targetScores.size());
        for (Map.Entry<TargetRef, BigDecimal> e : targetScores.entrySet()) {
            TargetRef target = e.getKey();
            BigDecimal value = e.getValue();
            Integer rank = rankMap.get(target);
            String grade = matchGrade(indicator, scheme, value, rank, allValues, targetScores.size());

            IndicatorResult r = IndicatorResult.draft(
                    indicator.getId(),
                    target.id,
                    namesByTarget.get(target),
                    periodKey,
                    value,
                    rank,
                    grade,
                    targetSubmissionIds.get(target),
                    new ArrayList<>(sectionIds));
            results.add(r);
        }
        return results;
    }

    // ── 收集 submissions ───────────────────────────────────────

    private CollectionResult collectSubmissions(
            Indicator indicator, List<Long> sectionIds,
            LocalDate periodStart, LocalDate periodEnd) {

        SubmissionDateField dateField = indicator.getSubmissionDateField() != null
                ? indicator.getSubmissionDateField() : SubmissionDateField.taskDate;
        Set<Long> sectionSet = new java.util.HashSet<>(sectionIds);

        CollectionResult out = new CollectionResult();

        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDateBetween(
                indicator.getProjectId(), periodStart, periodEnd);

        // 若按 completedAt, 扩大查询窗口 (taskDate 可能在 period 外, 但 completedAt 在 period 内).
        // 简化处理: 拉项目所有任务再按 completedAt 过滤. 性能 OK 因为 period 通常 ≤ 1 月.
        if (dateField == SubmissionDateField.completedAt) {
            tasks = taskRepository.findByProjectId(indicator.getProjectId());
        }

        for (InspTask task : tasks) {
            List<InspSubmission> subs = submissionRepository.findByTaskId(task.getId());
            for (InspSubmission sub : subs) {
                if (sub.getStatus() != SubmissionStatus.COMPLETED) continue;
                if (sub.getFinalScore() == null) continue;
                if (sub.getSectionId() == null || !sectionSet.contains(sub.getSectionId())) continue;
                if (sub.getTargetId() == null) continue;

                // 日期归属过滤
                if (!isInPeriod(sub, task, dateField, periodStart, periodEnd)) continue;

                TargetRef tref = new TargetRef(sub.getTargetId());
                out.byTarget.computeIfAbsent(tref, k -> new LinkedHashMap<>())
                        .computeIfAbsent(sub.getSectionId(), k -> new ArrayList<>())
                        .add(sub.getFinalScore());
                out.subIdsByTarget.computeIfAbsent(tref, k -> new TreeSet<>()).add(sub.getId());
                out.namesByTarget.putIfAbsent(tref, sub.getTargetName());
            }
        }
        return out;
    }

    private static final class CollectionResult {
        final Map<TargetRef, Map<Long, List<BigDecimal>>> byTarget = new LinkedHashMap<>();
        final Map<TargetRef, Set<Long>> subIdsByTarget = new LinkedHashMap<>();
        final Map<TargetRef, String> namesByTarget = new LinkedHashMap<>();
    }

    private boolean isInPeriod(InspSubmission sub, InspTask task, SubmissionDateField field,
                                LocalDate start, LocalDate end) {
        if (field == SubmissionDateField.completedAt) {
            LocalDateTime c = sub.getCompletedAt();
            if (c == null) return false;
            LocalDate d = c.toLocalDate();
            return !d.isBefore(start) && !d.isAfter(end);
        }
        // taskDate
        LocalDate d = task.getTaskDate();
        if (d == null) return false;
        return !d.isBefore(start) && !d.isAfter(end);
    }

    private boolean hasAnyMissingSection(Map<TargetRef, Map<Long, List<BigDecimal>>> bySectionByTarget,
                                          List<Long> sectionIds) {
        if (bySectionByTarget.isEmpty()) return true;
        for (Map<Long, List<BigDecimal>> bySection : bySectionByTarget.values()) {
            for (Long sid : sectionIds) {
                List<BigDecimal> vs = bySection.get(sid);
                if (vs == null || vs.isEmpty()) return true;
            }
        }
        return false;
    }

    // ── 跨 section 聚合 ─────────────────────────────────────────

    private BigDecimal aggregateAcrossSections(Indicator indicator, List<Long> sectionIds,
                                                Map<Long, List<BigDecimal>> bySection) {
        MissingPolicy policy = indicator.getMissingPolicy() != null
                ? indicator.getMissingPolicy() : MissingPolicy.IGNORE;

        // 先把每个 section 的多 submission 聚合成单值 (用 sourceAggregation 默认 AVG)
        Map<Long, BigDecimal> sectionValue = new LinkedHashMap<>();
        for (Long sid : sectionIds) {
            List<BigDecimal> vs = bySection.get(sid);
            if (vs == null || vs.isEmpty()) {
                switch (policy) {
                    case IGNORE -> {} // 跳过, 不放入
                    case ZERO -> sectionValue.put(sid, BigDecimal.ZERO);
                    case MAX -> {
                        BigDecimal max = computeSectionMaxAcross(bySection.values());
                        if (max != null) sectionValue.put(sid, max);
                    }
                    case WAIT -> {
                        // 顶层已 short-circuit, 这里防御性返回 null
                        return null;
                    }
                }
            } else {
                sectionValue.put(sid, aggregateScores(vs, indicator.getSourceAggregation()));
            }
        }

        if (sectionValue.isEmpty()) return null;

        // 按 weightsBySection 加权 (空=等权 AVG)
        Map<Long, BigDecimal> weights = indicator.getWeightsBySection();
        if (weights == null || weights.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (BigDecimal v : sectionValue.values()) sum = sum.add(v);
            return sum.divide(BigDecimal.valueOf(sectionValue.size()), 4, RoundingMode.HALF_UP);
        }
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalW = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : sectionValue.entrySet()) {
            BigDecimal w = weights.getOrDefault(e.getKey(), BigDecimal.ONE);
            weightedSum = weightedSum.add(e.getValue().multiply(w));
            totalW = totalW.add(w);
        }
        if (totalW.compareTo(BigDecimal.ZERO) == 0) return null;
        return weightedSum.divide(totalW, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeSectionMaxAcross(java.util.Collection<List<BigDecimal>> all) {
        BigDecimal max = null;
        for (List<BigDecimal> vs : all) {
            for (BigDecimal v : vs) {
                if (max == null || v.compareTo(max) > 0) max = v;
            }
        }
        return max;
    }

    private BigDecimal aggregateScores(List<BigDecimal> scores, String method) {
        if (scores == null || scores.isEmpty()) return BigDecimal.ZERO;
        return switch (method != null ? method : "AVG") {
            case "SUM" -> scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "MAX" -> scores.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            case "MIN" -> scores.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            case "LATEST" -> scores.get(scores.size() - 1);
            default -> {
                BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP);
            }
        };
    }

    // ── 排名 ────────────────────────────────────────────────────

    private Map<TargetRef, Integer> computeRanks(Map<TargetRef, BigDecimal> targetScores,
                                                  RankDirection direction) {
        Map<TargetRef, Integer> rankMap = new HashMap<>();
        if (direction == null) return rankMap; // 不排名

        List<Map.Entry<TargetRef, BigDecimal>> sorted = new ArrayList<>(targetScores.entrySet());
        sorted.sort((a, b) -> direction == RankDirection.ASC
                ? a.getValue().compareTo(b.getValue())
                : b.getValue().compareTo(a.getValue()));

        // 同分同 rank, 下一个跳号 (1,2,2,4)
        int rank = 0;
        int processed = 0;
        BigDecimal lastValue = null;
        for (Map.Entry<TargetRef, BigDecimal> e : sorted) {
            processed++;
            if (lastValue == null || e.getValue().compareTo(lastValue) != 0) {
                rank = processed;
                lastValue = e.getValue();
            }
            rankMap.put(e.getKey(), rank);
        }
        return rankMap;
    }

    // ── 等级映射 ────────────────────────────────────────────────

    private GradeScheme loadScheme(Long schemeId) {
        if (schemeId == null) return null;
        GradeScheme scheme = gradeSchemeRepository.findById(schemeId).orElse(null);
        if (scheme != null) {
            scheme.setGrades(gradeDefinitionRepository.findByGradeSchemeId(scheme.getId()));
        }
        return scheme;
    }

    /** 按 indicator.gradeThresholds JSON + evaluationMethod 匹配等级码. 失败返回 null. */
    private String matchGrade(Indicator indicator, GradeScheme scheme, BigDecimal value,
                               Integer rank, List<BigDecimal> allValues, int totalTargets) {
        if (scheme == null) return null;
        if (indicator.getGradeThresholds() == null || indicator.getGradeThresholds().isBlank()) {
            // 退化: 按 GradeDefinition 的 minValue/maxValue 命中
            for (GradeDefinition g : scheme.getGrades()) {
                if (inRange(value, g.getMinValue(), g.getMaxValue())) return g.getCode();
            }
            return null;
        }

        List<Threshold> thresholds;
        try {
            thresholds = Arrays.asList(new ObjectMapper()
                    .readValue(indicator.getGradeThresholds(), Threshold[].class));
        } catch (Exception ex) {
            log.warn("Indicator {} gradeThresholds 解析失败: {}", indicator.getId(), ex.getMessage());
            return null;
        }
        if (thresholds.isEmpty()) return null;

        String method = indicator.getEvaluationMethod() != null
                ? indicator.getEvaluationMethod() : "SCORE_RANGE";

        return switch (method) {
            case "RANK_COUNT" -> {
                if (rank == null) yield null;
                // 升序: rank<=阈值 命中
                List<Threshold> asc = new ArrayList<>(thresholds);
                asc.sort(Comparator.comparingDouble(t -> t.value));
                for (Threshold t : asc) if (rank <= t.value) yield t.gradeCode;
                yield asc.get(asc.size() - 1).gradeCode;
            }
            case "RANK_PERCENT" -> {
                if (rank == null || totalTargets == 0) yield null;
                double pct = (double) rank / totalTargets * 100;
                List<Threshold> asc = new ArrayList<>(thresholds);
                asc.sort(Comparator.comparingDouble(t -> t.value));
                for (Threshold t : asc) if (pct <= t.value) yield t.gradeCode;
                yield asc.get(asc.size() - 1).gradeCode;
            }
            default -> { // SCORE_RANGE / PERCENT_RANGE 等
                List<Threshold> desc = new ArrayList<>(thresholds);
                desc.sort((a, b) -> Double.compare(b.value, a.value));
                double v = value.doubleValue();
                for (Threshold t : desc) if (v >= t.value) yield t.gradeCode;
                yield desc.get(desc.size() - 1).gradeCode;
            }
        };
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) return false;
        boolean okMin = (min == null) || value.compareTo(min) >= 0;
        boolean okMax = (max == null) || value.compareTo(max) <= 0;
        return okMin && okMax;
    }

    // ── 内部类型 ────────────────────────────────────────────────

    /** target 标识 (id 为相等性主键). */
    private static final class TargetRef {
        final Long id;

        TargetRef(Long id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TargetRef t && Objects.equals(id, t.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }

    /** Jackson value-class — JSON {"gradeCode":"A","value":90}. */
    public static class Threshold {
        public String gradeCode;
        public double value;
    }
}
