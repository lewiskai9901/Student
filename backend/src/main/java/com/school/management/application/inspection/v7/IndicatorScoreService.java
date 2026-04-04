package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.domain.inspection.repository.v7.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorScoreService {

    private final IndicatorRepository indicatorRepository;
    private final IndicatorScoreRepository scoreRepository;
    private final InspSubmissionRepository submissionRepository;
    private final InspTaskRepository taskRepository;
    private final GradeSchemeRepository gradeSchemeRepository;
    private final GradeDefinitionRepository gradeDefinitionRepository;

    // ══════════════════════════════════════════════
    // 1. Full project computation for a period
    // ══════════════════════════════════════════════

    /**
     * Compute all indicator scores for a project in a given period.
     * Process order: leaf indicators first, then composite (bottom-up).
     */
    @Transactional
    public void computeAllForProject(Long projectId, LocalDate periodStart, LocalDate periodEnd) {
        List<Indicator> all = indicatorRepository.findByProjectId(projectId);
        if (all.isEmpty()) return;

        // Separate leaves and composites
        List<Indicator> leaves = all.stream().filter(Indicator::isLeaf).toList();
        List<Indicator> composites = all.stream().filter(Indicator::isComposite).toList();

        // Collect all distinct targets from submissions in the period
        Set<TargetInfo> targets = collectTargetsInPeriod(projectId, periodStart, periodEnd);

        // Step 1: compute all leaf indicators (two passes: first compute scores, then match grades)
        // We need two passes because RANK-based grade matching needs all target scores
        Map<Long, List<BigDecimal>> leafAllScores = new HashMap<>();
        for (Indicator leaf : leaves) {
            List<BigDecimal> allScoresForLeaf = new ArrayList<>();
            for (TargetInfo target : targets) {
                BigDecimal score = computeLeafScore(leaf, target, periodStart, periodEnd);
                if (score != null) allScoresForLeaf.add(score);
            }
            leafAllScores.put(leaf.getId(), allScoresForLeaf);
        }
        // Now update grades with full score lists available
        for (Indicator leaf : leaves) {
            List<BigDecimal> allScores = leafAllScores.getOrDefault(leaf.getId(), List.of());
            for (TargetInfo target : targets) {
                updateLeafGrade(leaf, target, periodStart, allScores);
            }
        }

        // Step 2: compute composites bottom-up (children before parents)
        // Sort by depth: deepest first
        List<Indicator> sortedComposites = sortCompositesByDepth(composites, all);
        Map<Long, List<BigDecimal>> compositeAllScores = new HashMap<>();
        for (Indicator composite : sortedComposites) {
            List<BigDecimal> allScoresForComposite = new ArrayList<>();
            for (TargetInfo target : targets) {
                BigDecimal score = computeCompositeScore(composite, target, periodStart, periodEnd);
                if (score != null) allScoresForComposite.add(score);
            }
            compositeAllScores.put(composite.getId(), allScoresForComposite);
        }
        // Update grades for composites
        for (Indicator composite : sortedComposites) {
            List<BigDecimal> allScores = compositeAllScores.getOrDefault(composite.getId(), List.of());
            for (TargetInfo target : targets) {
                updateCompositeGrade(composite, target, periodStart, allScores);
            }
        }

        log.info("Project {} indicator scores computed: period={} to {}, targets={}, indicators={}",
                projectId, periodStart, periodEnd, targets.size(), all.size());
    }

    // ══════════════════════════════════════════════
    // 2. Incremental computation on submission complete
    // ══════════════════════════════════════════════

    /**
     * Called after a submission is completed. Recomputes the relevant leaf indicator.
     * For PER_TASK indicators, periodStart = periodEnd = taskDate.
     */
    @Transactional
    public void computeOnSubmissionComplete(Long submissionId) {
        // Implementation: find submission → find task → find indicators by sectionId → compute leaf
        // Find which indicators reference this submission's section
        InspSubmission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getSectionId() == null) return;
        InspTask task = taskRepository.findById(submission.getTaskId()).orElse(null);
        if (task == null) return;

        List<Indicator> projectIndicators = indicatorRepository.findByProjectId(task.getProjectId());
        List<Indicator> relevantLeaves = projectIndicators.stream()
                .filter(i -> i.isLeaf() && submission.getSectionId().equals(i.getSourceSectionId()))
                .toList();

        for (Indicator leaf : relevantLeaves) {
            LocalDate taskDate = task.getTaskDate();
            LocalDate[] period = resolvePeriod(leaf.getEvaluationPeriod(), taskDate);
            TargetInfo target = new TargetInfo(submission.getTargetId(), submission.getTargetName(),
                    submission.getTargetType() != null ? submission.getTargetType().name() : null);
            BigDecimal leafScore = computeLeafScore(leaf, target, period[0], period[1]);

            // Collect all scores for this leaf indicator in the period for grade matching
            if (leafScore != null) {
                List<BigDecimal> allLeafScores = collectAllScoresForIndicator(leaf.getId(), period[0]);
                updateLeafGrade(leaf, target, period[0], allLeafScores);
            }

            // Also recompute parent composites up the chain
            recomputeAncestors(leaf, target, period[0], period[1], projectIndicators);
        }
    }

    // ══════════════════════════════════════════════
    // 3. Core computation methods
    // ══════════════════════════════════════════════

    /**
     * Compute a LEAF indicator's score for a target in a period.
     * Aggregates all completed submissions for the source section within the period.
     * Returns the aggregated score, or null if no data.
     */
    private BigDecimal computeLeafScore(Indicator leaf, TargetInfo target,
                                         LocalDate periodStart, LocalDate periodEnd) {
        // Get all tasks in the period for this project
        List<BigDecimal> scores = new ArrayList<>();
        LocalDate date = periodStart;
        while (!date.isAfter(periodEnd)) {
            List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDate(leaf.getProjectId(), date);
            for (InspTask task : tasks) {
                List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId());
                for (InspSubmission sub : submissions) {
                    if (sub.getStatus() == SubmissionStatus.COMPLETED
                            && sub.getFinalScore() != null
                            && leaf.getSourceSectionId().equals(sub.getSectionId())
                            && target.id.equals(sub.getTargetId())) {
                        scores.add(sub.getFinalScore());
                    }
                }
            }
            date = date.plusDays(1);
        }

        if (scores.isEmpty()) return null;

        BigDecimal aggregated = aggregate(scores, leaf.getSourceAggregation());

        IndicatorScore record = scoreRepository
                .findByIndicatorAndTargetAndPeriod(leaf.getId(), target.id, periodStart)
                .orElse(IndicatorScore.create(leaf.getId(), target.id, target.name, target.type,
                        periodStart, periodEnd));
        record.updateScore(aggregated, null, null, null, scores.size(), null);
        scoreRepository.save(record);
        return aggregated;
    }

    /**
     * Update grade for a LEAF indicator score after all target scores are computed.
     */
    private void updateLeafGrade(Indicator leaf, TargetInfo target,
                                  LocalDate periodStart, List<BigDecimal> allScores) {
        IndicatorScore record = scoreRepository
                .findByIndicatorAndTargetAndPeriod(leaf.getId(), target.id, periodStart)
                .orElse(null);
        if (record == null || record.getScore() == null) return;

        GradeMatch grade = matchGradeForIndicator(leaf, record.getScore(), null, allScores);
        record.updateScore(record.getScore(), grade.code, grade.name, grade.color,
                record.getSourceCount(), record.getDetail());
        scoreRepository.save(record);
    }

    /**
     * Compute a COMPOSITE indicator's score from its children's IndicatorScores.
     * Returns the computed score, or null if no data.
     */
    private BigDecimal computeCompositeScore(Indicator composite, TargetInfo target,
                                              LocalDate periodStart, LocalDate periodEnd) {
        List<Indicator> children = indicatorRepository.findByParentIndicatorId(composite.getId());
        if (children.isEmpty()) return null;

        List<WeightedScore> childScores = new ArrayList<>();
        for (Indicator child : children) {
            IndicatorScore childScore = scoreRepository
                    .findByIndicatorAndTargetAndPeriod(child.getId(), target.id, periodStart)
                    .orElse(null);

            if (childScore != null && childScore.getScore() != null) {
                childScores.add(new WeightedScore(childScore.getScore(), 1));
            } else {
                // Handle missing
                String policy = composite.getMissingPolicy();
                if ("CARRY_FORWARD".equals(policy)) {
                    // Use most recent score for this child+target
                    List<IndicatorScore> history = scoreRepository.findByIndicatorId(child.getId());
                    IndicatorScore latest = history.stream()
                            .filter(s -> target.id.equals(s.getTargetId()) && s.getScore() != null)
                            .max(Comparator.comparing(IndicatorScore::getPeriodStart))
                            .orElse(null);
                    if (latest != null) {
                        childScores.add(new WeightedScore(latest.getScore(), 1));
                    }
                    // else SKIP implicitly
                } else if ("MARK_INCOMPLETE".equals(policy)) {
                    // If any child is missing, don't compute this composite
                    return null;
                }
                // SKIP: just don't add, weight will be redistributed
            }
        }

        if (childScores.isEmpty()) return null;

        BigDecimal result = aggregateComposite(childScores, composite.getCompositeAggregation());

        IndicatorScore record = scoreRepository
                .findByIndicatorAndTargetAndPeriod(composite.getId(), target.id, periodStart)
                .orElse(IndicatorScore.create(composite.getId(), target.id, target.name, target.type,
                        periodStart, periodEnd));
        record.updateScore(result, null, null, null, childScores.size(), null);
        scoreRepository.save(record);
        return result;
    }

    /**
     * Update grade for a COMPOSITE indicator score after all target scores are computed.
     */
    private void updateCompositeGrade(Indicator composite, TargetInfo target,
                                       LocalDate periodStart, List<BigDecimal> allScores) {
        IndicatorScore record = scoreRepository
                .findByIndicatorAndTargetAndPeriod(composite.getId(), target.id, periodStart)
                .orElse(null);
        if (record == null || record.getScore() == null) return;

        GradeMatch grade = matchGradeForIndicator(composite, record.getScore(), null, allScores);
        record.updateScore(record.getScore(), grade.code, grade.name, grade.color,
                record.getSourceCount(), record.getDetail());
        scoreRepository.save(record);
    }

    // ══════════════════════════════════════════════
    // 4. Helper methods
    // ══════════════════════════════════════════════

    /** Aggregate a list of scores by the given method */
    private BigDecimal aggregate(List<BigDecimal> scores, String method) {
        if (scores.isEmpty()) return BigDecimal.ZERO;
        return switch (method != null ? method : "AVG") {
            case "SUM" -> scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "MAX" -> scores.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            case "MIN" -> scores.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            case "LATEST" -> scores.get(scores.size() - 1);
            default -> { // AVG
                BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            }
        };
    }

    /** Aggregate weighted child scores */
    private BigDecimal aggregateComposite(List<WeightedScore> scores, String method) {
        if (scores.isEmpty()) return BigDecimal.ZERO;
        return switch (method != null ? method : "WEIGHTED_AVG") {
            case "SUM" -> scores.stream().map(s -> s.score).reduce(BigDecimal.ZERO, BigDecimal::add);
            case "AVG" -> {
                BigDecimal sum = scores.stream().map(s -> s.score).reduce(BigDecimal.ZERO, BigDecimal::add);
                yield sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
            }
            case "MAX" -> scores.stream().map(s -> s.score).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            case "MIN" -> scores.stream().map(s -> s.score).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            default -> { // WEIGHTED_AVG
                int totalWeight = scores.stream().mapToInt(s -> s.weight).sum();
                if (totalWeight == 0) yield BigDecimal.ZERO;
                BigDecimal weightedSum = scores.stream()
                        .map(s -> s.score.multiply(BigDecimal.valueOf(s.weight)))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                yield weightedSum.divide(BigDecimal.valueOf(totalWeight), 2, RoundingMode.HALF_UP);
            }
        };
    }

    /**
     * Match score to grade using indicator's evaluation method and thresholds.
     * For RANK-based methods, allScores is the full list of all target scores to compute rankings.
     */
    private GradeMatch matchGradeForIndicator(Indicator indicator, BigDecimal score,
                                               BigDecimal maxScore, List<BigDecimal> allScores) {
        if (indicator.getGradeSchemeId() == null || indicator.getGradeThresholds() == null) {
            return GradeMatch.EMPTY;
        }

        // Load grade scheme for display info
        GradeScheme scheme = gradeSchemeRepository.findById(indicator.getGradeSchemeId()).orElse(null);
        if (scheme == null) return GradeMatch.EMPTY;
        scheme.setGrades(gradeDefinitionRepository.findByGradeSchemeId(scheme.getId()));

        // Parse thresholds
        List<GradeThreshold> thresholds;
        try {
            thresholds = Arrays.asList(new ObjectMapper()
                    .readValue(indicator.getGradeThresholds(), GradeThreshold[].class));
        } catch (Exception e) {
            return GradeMatch.EMPTY;
        }

        // Sort thresholds descending by value (highest first)
        thresholds = new ArrayList<>(thresholds);
        thresholds.sort((a, b) -> Double.compare(b.value, a.value));

        String method = indicator.getEvaluationMethod();
        double compareValue;

        switch (method != null ? method : "PERCENT_RANGE") {
            case "SCORE_RANGE":
                compareValue = score.doubleValue();
                break;
            case "PERCENT_RANGE":
                if (maxScore == null || maxScore.doubleValue() == 0) compareValue = score.doubleValue();
                else compareValue = score.doubleValue() / maxScore.doubleValue() * 100;
                break;
            case "RANK_COUNT": {
                long rank = allScores.stream().filter(s -> s.compareTo(score) > 0).count() + 1;
                thresholds.sort((a, b) -> Double.compare(a.value, b.value));
                for (GradeThreshold t : thresholds) {
                    if (rank <= t.value && t.meetsMinScore(score.doubleValue())) {
                        return findGradeDisplay(scheme, t.gradeCode);
                    }
                }
                return GradeMatch.EMPTY;
            }
            case "RANK_PERCENT": {
                if (allScores.isEmpty()) return GradeMatch.EMPTY;
                long rankP = allScores.stream().filter(s -> s.compareTo(score) > 0).count() + 1;
                double percentile = (double) rankP / allScores.size() * 100;
                thresholds.sort((a, b) -> Double.compare(a.value, b.value));
                for (GradeThreshold t : thresholds) {
                    if (percentile <= t.value && t.meetsMinScore(score.doubleValue())) {
                        return findGradeDisplay(scheme, t.gradeCode);
                    }
                }
                return GradeMatch.EMPTY;
            }
            default:
                compareValue = score.doubleValue();
        }

        // For SCORE_RANGE and PERCENT_RANGE: find first threshold where compareValue >= threshold
        for (GradeThreshold t : thresholds) {
            if (compareValue >= t.value) {
                return findGradeDisplay(scheme, t.gradeCode);
            }
        }
        // If no match, use last grade
        if (!thresholds.isEmpty()) {
            return findGradeDisplay(scheme, thresholds.get(thresholds.size() - 1).gradeCode);
        }
        return GradeMatch.EMPTY;
    }

    private GradeMatch findGradeDisplay(GradeScheme scheme, String gradeCode) {
        for (GradeDefinition g : scheme.getGrades()) {
            if (g.getCode().equals(gradeCode)) {
                return new GradeMatch(g.getCode(), g.getName(), g.getColor());
            }
        }
        return GradeMatch.EMPTY;
    }

    /** Collect all target scores for an indicator in a period (for RANK-based grade matching) */
    private List<BigDecimal> collectAllScoresForIndicator(Long indicatorId, LocalDate periodStart) {
        List<IndicatorScore> scores = scoreRepository.findByIndicatorId(indicatorId);
        return scores.stream()
                .filter(s -> s.getPeriodStart() != null && s.getPeriodStart().equals(periodStart) && s.getScore() != null)
                .map(IndicatorScore::getScore)
                .collect(Collectors.toList());
    }

    /** Collect all distinct targets from submissions in a period */
    private Set<TargetInfo> collectTargetsInPeriod(Long projectId, LocalDate start, LocalDate end) {
        Set<TargetInfo> targets = new LinkedHashSet<>();
        LocalDate date = start;
        while (!date.isAfter(end)) {
            List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDate(projectId, date);
            for (InspTask task : tasks) {
                List<InspSubmission> subs = submissionRepository.findByTaskId(task.getId());
                for (InspSubmission sub : subs) {
                    if (sub.getTargetId() != null) {
                        targets.add(new TargetInfo(sub.getTargetId(), sub.getTargetName(),
                                sub.getTargetType() != null ? sub.getTargetType().name() : null));
                    }
                }
            }
            date = date.plusDays(1);
        }
        return targets;
    }

    /** Resolve period boundaries from evaluationPeriod and a reference date */
    private LocalDate[] resolvePeriod(String evaluationPeriod, LocalDate refDate) {
        return switch (evaluationPeriod != null ? evaluationPeriod : "PER_TASK") {
            case "DAILY" -> new LocalDate[]{ refDate, refDate };
            case "WEEKLY" -> {
                LocalDate monday = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate sunday = monday.plusDays(6);
                yield new LocalDate[]{ monday, sunday };
            }
            case "MONTHLY" -> {
                LocalDate first = refDate.withDayOfMonth(1);
                LocalDate last = refDate.with(TemporalAdjusters.lastDayOfMonth());
                yield new LocalDate[]{ first, last };
            }
            default -> new LocalDate[]{ refDate, refDate }; // PER_TASK
        };
    }

    /** Recompute all ancestor composites up from a leaf */
    private void recomputeAncestors(Indicator child, TargetInfo target,
                                     LocalDate periodStart, LocalDate periodEnd,
                                     List<Indicator> allIndicators) {
        if (child.getParentIndicatorId() == null) return;
        Indicator parent = allIndicators.stream()
                .filter(i -> i.getId().equals(child.getParentIndicatorId()))
                .findFirst().orElse(null);
        if (parent == null || !parent.isComposite()) return;

        // Resolve the parent's period (may be different from child's)
        LocalDate[] parentPeriod = resolvePeriod(parent.getEvaluationPeriod(), periodStart);
        BigDecimal compositeScore = computeCompositeScore(parent, target, parentPeriod[0], parentPeriod[1]);

        // Update grade for this composite
        if (compositeScore != null) {
            List<BigDecimal> allCompositeScores = collectAllScoresForIndicator(parent.getId(), parentPeriod[0]);
            updateCompositeGrade(parent, target, parentPeriod[0], allCompositeScores);
        }

        // Continue up
        recomputeAncestors(parent, target, parentPeriod[0], parentPeriod[1], allIndicators);
    }

    /** Sort composites so children come before parents (deepest first) */
    private List<Indicator> sortCompositesByDepth(List<Indicator> composites, List<Indicator> all) {
        // Simple topological sort: indicators with no composite children first
        Map<Long, Indicator> map = all.stream().collect(Collectors.toMap(Indicator::getId, i -> i));
        List<Indicator> sorted = new ArrayList<>(composites);
        sorted.sort((a, b) -> {
            // If a is ancestor of b, a should come after b
            if (isAncestor(a.getId(), b.getId(), map)) return 1;
            if (isAncestor(b.getId(), a.getId(), map)) return -1;
            return 0;
        });
        return sorted;
    }

    private boolean isAncestor(Long ancestorId, Long descendantId, Map<Long, Indicator> map) {
        Indicator current = map.get(descendantId);
        while (current != null && current.getParentIndicatorId() != null) {
            if (current.getParentIndicatorId().equals(ancestorId)) return true;
            current = map.get(current.getParentIndicatorId());
        }
        return false;
    }

    // ── Value types ──

    private record TargetInfo(Long id, String name, String type) {
        @Override public boolean equals(Object o) {
            return o instanceof TargetInfo t && Objects.equals(id, t.id);
        }
        @Override public int hashCode() { return Objects.hashCode(id); }
    }

    private record WeightedScore(BigDecimal score, int weight) {}

    private record GradeMatch(String code, String name, String color) {
        static final GradeMatch EMPTY = new GradeMatch(null, null, null);
    }

    private record GradeThreshold(String gradeCode, double value, Double minScore) {
        boolean meetsMinScore(double actualScore) {
            return minScore == null || actualScore >= minScore;
        }
    }
}
