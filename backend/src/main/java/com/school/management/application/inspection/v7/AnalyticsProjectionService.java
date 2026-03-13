package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.event.v7.*;
import com.school.management.domain.inspection.model.v7.analytics.*;
import com.school.management.domain.inspection.model.v7.execution.InspSubmission;
import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V7 分析投影服务 (CQRS 写侧)
 * 监听 TaskPublishedEvent → 重建当日汇总
 * 提供手动重建 daily/period 的能力
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AnalyticsProjectionService {

    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final DailySummaryRepository dailySummaryRepository;
    private final PeriodSummaryRepository periodSummaryRepository;
    private final CorrectiveCaseRepository correctiveCaseRepository;
    private final InspectorSummaryRepository inspectorSummaryRepository;
    private final ItemFrequencySummaryRepository itemFrequencySummaryRepository;
    private final CorrectiveSummaryRepository correctiveSummaryRepository;
    private final SpringDomainEventPublisher eventPublisher;

    // ========== Event-Driven Projection ==========

    @Async
    @EventListener
    public void onTaskPublished(TaskPublishedEvent event) {
        log.info("Analytics projection triggered by TaskPublishedEvent: taskId={}, projectId={}",
                event.getTaskId(), event.getProjectId());
        try {
            InspTask task = taskRepository.findById(event.getTaskId()).orElse(null);
            if (task == null || task.getTaskDate() == null) return;

            rebuildDailySummary(event.getProjectId(), task.getTaskDate());
        } catch (Exception e) {
            log.error("Failed to project analytics for task {}: {}", event.getTaskId(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onSubmissionCompleted(SubmissionCompletedEvent event) {
        log.info("Analytics projection: SubmissionCompletedEvent submissionId={}", event.getSubmissionId());
        try {
            // Find the submission and task
            InspSubmission submission = submissionRepository.findById(event.getSubmissionId()).orElse(null);
            if (submission == null) return;
            InspTask task = taskRepository.findById(event.getTaskId()).orElse(null);
            if (task == null) return;

            // Rebuild daily summary for the task date
            if (task.getTaskDate() != null) {
                rebuildDailySummary(task.getProjectId(), task.getTaskDate());
            }

            // Update inspector summary
            if (task.getInspectorId() != null) {
                updateInspectorSummary(task.getProjectId(), task.getInspectorId(), task.getInspectorName());
            }
        } catch (Exception e) {
            log.error("Failed to project SubmissionCompletedEvent: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onCorrectiveCaseCreated(CorrectiveCaseCreatedEvent event) {
        log.info("Analytics projection: CorrectiveCaseCreatedEvent caseId={}", event.getCaseId());
        try {
            updateCorrectiveSummaryForProject(event);
        } catch (Exception e) {
            log.error("Failed to project CorrectiveCaseCreatedEvent: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onCaseClosed(CaseClosedEvent event) {
        log.info("Analytics projection: CaseClosedEvent caseId={}", event.getCaseId());
        try {
            updateCorrectiveSummaryForCase(event.getCaseId());
        } catch (Exception e) {
            log.error("Failed to project CaseClosedEvent: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onEffectivenessFailed(EffectivenessFailedEvent event) {
        log.info("Analytics projection: EffectivenessFailedEvent caseId={}", event.getCaseId());
        try {
            updateCorrectiveSummaryForCase(event.getCaseId());
        } catch (Exception e) {
            log.error("Failed to project EffectivenessFailedEvent: {}", e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void onTaskCancelled(TaskCancelledEvent event) {
        log.info("Analytics projection: TaskCancelledEvent taskId={}", event.getTaskId());
        try {
            InspTask task = taskRepository.findById(event.getTaskId()).orElse(null);
            if (task != null && task.getInspectorId() != null) {
                updateInspectorSummary(task.getProjectId(), task.getInspectorId(), task.getInspectorName());
            }
        } catch (Exception e) {
            log.error("Failed to project TaskCancelledEvent: {}", e.getMessage(), e);
        }
    }

    // ========== Manual Rebuild ==========

    @Transactional
    public void rebuildDailySummary(Long projectId, LocalDate date) {
        log.info("Rebuilding daily summary: projectId={}, date={}", projectId, date);

        // Find all published tasks for this project on this date
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDate(projectId, date).stream()
                .filter(t -> "PUBLISHED".equals(t.getStatus().name()))
                .toList();

        // Collect all submissions from these tasks
        Map<String, List<InspSubmission>> groupedByTarget = new HashMap<>();
        for (InspTask task : tasks) {
            List<InspSubmission> submissions = submissionRepository.findByTaskId(task.getId()).stream()
                    .filter(s -> "COMPLETED".equals(s.getStatus().name()))
                    .toList();
            for (InspSubmission sub : submissions) {
                String key = sub.getTargetType() + ":" + sub.getTargetId();
                groupedByTarget.computeIfAbsent(key, k -> new ArrayList<>()).add(sub);
            }
        }

        // Delete old summaries for this date
        dailySummaryRepository.deleteByProjectAndDate(projectId, date);

        // Build summaries per target
        List<DailySummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<InspSubmission>> entry : groupedByTarget.entrySet()) {
            List<InspSubmission> subs = entry.getValue();
            InspSubmission first = subs.get(0);

            DailySummary summary = new DailySummary();
            summary.setProjectId(projectId);
            summary.setSummaryDate(date);
            summary.setTargetType(first.getTargetType() != null ? first.getTargetType().name() : null);
            summary.setTargetId(first.getTargetId());
            summary.setTargetName(first.getTargetName());
            summary.setOrgUnitId(first.getOrgUnitId());
            summary.setOrgUnitName(first.getOrgUnitName());
            summary.setInspectionCount(subs.size());

            // Calculate scores
            List<BigDecimal> scores = subs.stream()
                    .map(InspSubmission::getFinalScore)
                    .filter(Objects::nonNull)
                    .toList();

            if (!scores.isEmpty()) {
                BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                summary.setAvgScore(sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP));
                summary.setMinScore(scores.stream().min(BigDecimal::compareTo).orElse(null));
                summary.setMaxScore(scores.stream().max(BigDecimal::compareTo).orElse(null));
            }

            // Deductions & bonuses
            summary.setTotalDeductions(subs.stream()
                    .map(InspSubmission::getDeductionTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalBonuses(subs.stream()
                    .map(InspSubmission::getBonusTotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            // Pass/fail
            summary.setPassCount((int) subs.stream().filter(s -> Boolean.TRUE.equals(s.getPassed())).count());
            summary.setFailCount((int) subs.stream().filter(s -> Boolean.FALSE.equals(s.getPassed())).count());

            summary.setGrade(first.getGrade());
            summary.setCreatedAt(LocalDateTime.now());

            summaries.add(summary);
        }

        // Calculate ranking by avgScore descending
        summaries.sort((a, b) -> {
            if (a.getAvgScore() == null && b.getAvgScore() == null) return 0;
            if (a.getAvgScore() == null) return 1;
            if (b.getAvgScore() == null) return -1;
            return b.getAvgScore().compareTo(a.getAvgScore());
        });
        for (int i = 0; i < summaries.size(); i++) {
            summaries.get(i).setRanking(i + 1);
        }

        // Save all
        for (DailySummary s : summaries) {
            dailySummaryRepository.save(s);
            eventPublisher.publish(new DailySummaryUpdatedEvent(
                    projectId, date, s.getTargetType(), s.getTargetId()));
        }

        log.info("Rebuilt {} daily summaries for project {} on {}", summaries.size(), projectId, date);
    }

    @Transactional
    public void rebuildPeriodSummary(Long projectId, PeriodType periodType, LocalDate periodStart) {
        LocalDate periodEnd = calculatePeriodEnd(periodType, periodStart);
        log.info("Rebuilding period summary: projectId={}, type={}, {}-{}", projectId, periodType, periodStart, periodEnd);

        // Get all daily summaries in this period
        List<DailySummary> dailies = dailySummaryRepository.findByProjectAndDateRange(projectId, periodStart, periodEnd);

        // Group by target
        Map<String, List<DailySummary>> grouped = dailies.stream()
                .collect(Collectors.groupingBy(d -> d.getTargetType() + ":" + d.getTargetId()));

        // Delete old
        periodSummaryRepository.deleteByProjectAndPeriod(projectId, periodType, periodStart);

        List<PeriodSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<DailySummary>> entry : grouped.entrySet()) {
            List<DailySummary> days = entry.getValue();
            DailySummary first = days.get(0);

            PeriodSummary ps = new PeriodSummary();
            ps.setProjectId(projectId);
            ps.setPeriodType(periodType);
            ps.setPeriodStart(periodStart);
            ps.setPeriodEnd(periodEnd);
            ps.setTargetType(first.getTargetType());
            ps.setTargetId(first.getTargetId());
            ps.setTargetName(first.getTargetName());
            ps.setOrgUnitId(first.getOrgUnitId());
            ps.setOrgUnitName(first.getOrgUnitName());
            ps.setInspectionDays(days.size());

            // Aggregate scores
            List<BigDecimal> avgScores = days.stream()
                    .map(DailySummary::getAvgScore)
                    .filter(Objects::nonNull)
                    .toList();

            if (!avgScores.isEmpty()) {
                BigDecimal sum = avgScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                ps.setAvgScore(sum.divide(BigDecimal.valueOf(avgScores.size()), 2, RoundingMode.HALF_UP));
                ps.setMinScore(avgScores.stream().min(BigDecimal::compareTo).orElse(null));
                ps.setMaxScore(avgScores.stream().max(BigDecimal::compareTo).orElse(null));

                // Standard deviation
                if (avgScores.size() > 1) {
                    BigDecimal mean = ps.getAvgScore();
                    BigDecimal variance = avgScores.stream()
                            .map(s -> s.subtract(mean).pow(2))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(avgScores.size()), 4, RoundingMode.HALF_UP);
                    ps.setScoreStdDev(BigDecimal.valueOf(Math.sqrt(variance.doubleValue()))
                            .setScale(4, RoundingMode.HALF_UP));
                }
            }

            // Trend: compare first half vs second half
            if (avgScores.size() >= 2) {
                int mid = avgScores.size() / 2;
                BigDecimal firstHalfAvg = avgScores.subList(0, mid).stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(mid), 2, RoundingMode.HALF_UP);
                BigDecimal secondHalfAvg = avgScores.subList(mid, avgScores.size()).stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(avgScores.size() - mid), 2, RoundingMode.HALF_UP);

                int cmp = secondHalfAvg.compareTo(firstHalfAvg);
                if (cmp > 0) {
                    ps.setTrendDirection(TrendDirection.UP);
                } else if (cmp < 0) {
                    ps.setTrendDirection(TrendDirection.DOWN);
                } else {
                    ps.setTrendDirection(TrendDirection.STABLE);
                }

                if (firstHalfAvg.compareTo(BigDecimal.ZERO) > 0) {
                    ps.setTrendPercent(secondHalfAvg.subtract(firstHalfAvg)
                            .divide(firstHalfAvg, 2, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)));
                }
            }

            // Corrective counts from corrective case repository
            var cases = correctiveCaseRepository.findByProjectId(projectId);
            ps.setCorrectiveCount(cases.size());
            ps.setCorrectiveClosedCount((int) cases.stream()
                    .filter(c -> "CLOSED".equals(c.getStatus().name()) || "VERIFIED".equals(c.getStatus().name()))
                    .count());

            ps.setCreatedAt(LocalDateTime.now());
            summaries.add(ps);
        }

        // Ranking
        summaries.sort((a, b) -> {
            if (a.getAvgScore() == null && b.getAvgScore() == null) return 0;
            if (a.getAvgScore() == null) return 1;
            if (b.getAvgScore() == null) return -1;
            return b.getAvgScore().compareTo(a.getAvgScore());
        });
        for (int i = 0; i < summaries.size(); i++) {
            summaries.get(i).setRanking(i + 1);
        }

        for (PeriodSummary ps : summaries) {
            periodSummaryRepository.save(ps);
        }

        eventPublisher.publish(new PeriodSummaryCalculatedEvent(
                projectId, periodType.name(), periodStart, periodEnd));

        log.info("Rebuilt {} period summaries for project {} ({} {}-{})",
                summaries.size(), projectId, periodType, periodStart, periodEnd);
    }

    private LocalDate calculatePeriodEnd(PeriodType periodType, LocalDate start) {
        return switch (periodType) {
            case WEEKLY -> start.plusWeeks(1).minusDays(1);
            case MONTHLY -> start.with(TemporalAdjusters.lastDayOfMonth());
            case QUARTERLY -> start.plusMonths(3).minusDays(1);
            case YEARLY -> start.with(TemporalAdjusters.lastDayOfYear());
        };
    }

    // ========== Inspector & Corrective Summary Projections ==========

    @Transactional
    public void updateInspectorSummary(Long projectId, Long inspectorId, String inspectorName) {
        // Get current weekly period
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        PeriodType periodType = PeriodType.WEEKLY;

        // Find or create
        InspectorSummary summary = inspectorSummaryRepository
                .findByInspectorAndPeriod(projectId, inspectorId, periodType, weekStart)
                .orElseGet(() -> {
                    InspectorSummary s = new InspectorSummary();
                    s.setProjectId(projectId);
                    s.setInspectorId(inspectorId);
                    s.setInspectorName(inspectorName);
                    s.setPeriodType(periodType);
                    s.setPeriodStart(weekStart);
                    s.setPeriodEnd(weekEnd);
                    return s;
                });

        // Count tasks for this inspector in period
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDate(projectId, weekStart).stream()
                .filter(t -> inspectorId.equals(t.getInspectorId()))
                .toList();

        summary.setTotalTasks(tasks.size());
        summary.setCompletedTasks((int) tasks.stream()
                .filter(t -> "COMPLETED".equals(t.getStatus().name()) || "REVIEWED".equals(t.getStatus().name()))
                .count());
        summary.setCancelledTasks((int) tasks.stream()
                .filter(t -> "CANCELLED".equals(t.getStatus().name()))
                .count());
        summary.setExpiredTasks((int) tasks.stream()
                .filter(t -> "EXPIRED".equals(t.getStatus().name()))
                .count());

        // Count submissions
        int totalSubs = 0;
        int flaggedSubs = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        int scoredCount = 0;

        for (InspTask task : tasks) {
            List<InspSubmission> subs = submissionRepository.findByTaskId(task.getId());
            totalSubs += subs.size();
            flaggedSubs += (int) subs.stream().filter(s -> Boolean.FALSE.equals(s.getPassed())).count();
            for (InspSubmission sub : subs) {
                if (sub.getFinalScore() != null) {
                    totalScore = totalScore.add(sub.getFinalScore());
                    scoredCount++;
                }
            }
        }
        summary.setTotalSubmissions(totalSubs);
        summary.setFlaggedSubmissions(flaggedSubs);
        if (scoredCount > 0) {
            summary.setAvgScore(totalScore.divide(BigDecimal.valueOf(scoredCount), 2, RoundingMode.HALF_UP));
        }
        if (totalSubs > 0) {
            summary.setComplianceRate(BigDecimal.valueOf(totalSubs - flaggedSubs)
                    .divide(BigDecimal.valueOf(totalSubs), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)));
        }
        summary.setUpdatedAt(LocalDateTime.now());
        if (summary.getCreatedAt() == null) summary.setCreatedAt(LocalDateTime.now());

        inspectorSummaryRepository.save(summary);
        log.info("Updated InspectorSummary: projectId={}, inspectorId={}", projectId, inspectorId);
    }

    private void updateCorrectiveSummaryForProject(CorrectiveCaseCreatedEvent event) {
        if (event.getCaseId() == null) return;
        var c = correctiveCaseRepository.findById(event.getCaseId()).orElse(null);
        if (c == null || c.getProjectId() == null) return;
        rebuildCorrectiveSummary(c.getProjectId());
    }

    private void updateCorrectiveSummaryForCase(Long caseId) {
        if (caseId == null) return;
        var c = correctiveCaseRepository.findById(caseId).orElse(null);
        if (c == null || c.getProjectId() == null) return;
        rebuildCorrectiveSummary(c.getProjectId());
    }

    @Transactional
    public void rebuildCorrectiveSummary(Long projectId) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        PeriodType periodType = PeriodType.WEEKLY;

        List<com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase> cases =
                correctiveCaseRepository.findByProjectId(projectId);

        CorrectiveSummary summary = correctiveSummaryRepository
                .findByProjectAndPeriod(projectId, periodType, weekStart)
                .orElseGet(() -> {
                    CorrectiveSummary s = new CorrectiveSummary();
                    s.setProjectId(projectId);
                    s.setPeriodType(periodType);
                    s.setPeriodStart(weekStart);
                    s.setPeriodEnd(weekEnd);
                    return s;
                });

        summary.setTotalCases(cases.size());
        summary.setOpenCases((int) cases.stream().filter(c -> "OPEN".equals(c.getStatus().name())).count());
        summary.setClosedCases((int) cases.stream().filter(c -> "CLOSED".equals(c.getStatus().name()) || "VERIFIED".equals(c.getStatus().name())).count());
        summary.setOverdueCases((int) cases.stream().filter(com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase::isOverdue).count());
        summary.setAvgResolutionDays(BigDecimal.ZERO); // simplified
        summary.setUpdatedAt(LocalDateTime.now());
        if (summary.getCreatedAt() == null) summary.setCreatedAt(LocalDateTime.now());

        correctiveSummaryRepository.save(summary);
        log.info("Rebuilt CorrectiveSummary: projectId={}", projectId);
    }
}
