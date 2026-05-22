package com.school.management.application.inspection;

import com.school.management.domain.inspection.event.PeriodSummaryCalculatedEvent;
import com.school.management.domain.inspection.model.analytics.PeriodSummary;
import com.school.management.domain.inspection.model.rating.InspRatingLink;
import com.school.management.domain.inspection.repository.InspRatingLinkRepository;
import com.school.management.domain.inspection.repository.PeriodSummaryRepository;
import com.school.management.domain.rating.model.*;
import com.school.management.domain.rating.repository.RatingConfigRepository;
import com.school.management.domain.rating.repository.RatingResultRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 检查平台 - 评级计算处理器
 * 监听 PeriodSummaryCalculatedEvent，自动计算评级结果
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InspRatingCalculationHandler {

    private final InspRatingLinkRepository linkRepository;
    private final PeriodSummaryRepository periodSummaryRepository;
    private final RatingConfigRepository ratingConfigRepository;
    private final RatingResultRepository ratingResultRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * P1#8: 监听器方法本身标 {@code @Transactional}.
     *
     * <p>原实现 {@code @Async} 线程内 {@code this.calculateRatings()} 是自调用,
     * Spring AOP 代理无法拦截 → {@code calculateRatings} 上的 {@code @Transactional}
     * 完全失效, 评级结果可能部分落库. 直接给监听器方法加事务即可 (它本身
     * 不抢已提交的发起方事务 — AFTER_COMMIT + @Async 已是独立线程, 此处新开事务).
     *
     * <p>注意: try-catch 仍保留 — 但事务边界在 catch <b>之内</b>会因异常被标记
     * rollback. 这里 catch 在事务方法<b>外层</b>包不住, 因此把异常处理下沉到
     * {@code calculateRatings} 内部不可行; 改为让本方法整体在事务中执行,
     * 任一 link 失败则整批评级回滚, 语义清晰.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPeriodSummaryCalculated(PeriodSummaryCalculatedEvent event) {
        log.info("Rating calculation triggered: projectId={}, periodType={}, period={}-{}",
                event.getProjectId(), event.getPeriodType(), event.getPeriodStart(), event.getPeriodEnd());
        calculateRatings(event.getProjectId(), event.getPeriodType(),
                event.getPeriodStart(), event.getPeriodEnd());
    }

    @Transactional
    public void calculateRatings(Long projectId, String periodType,
                                  LocalDate periodStart, LocalDate periodEnd) {
        // Find all rating links for this project and period type
        List<InspRatingLink> links = linkRepository.findByProjectIdAndPeriodType(projectId, periodType);
        if (links.isEmpty()) {
            log.debug("No rating links found for project {} with period type {}", projectId, periodType);
            return;
        }

        // Get period summaries ranked by avgScore
        var v7PeriodType = com.school.management.domain.inspection.model.analytics.PeriodType.valueOf(periodType);
        List<PeriodSummary> summaries = periodSummaryRepository
                .findByProjectAndPeriod(projectId, v7PeriodType, periodStart);

        if (summaries.isEmpty()) {
            log.debug("No period summaries found for project {} period {}", projectId, periodStart);
            return;
        }

        // Sort by avgScore descending (for TOP-based) or ascending (for BOTTOM-based)
        List<PeriodSummary> ranked = new ArrayList<>(summaries);
        ranked.sort(Comparator.comparing(
                (PeriodSummary s) -> s.getAvgScore() != null ? s.getAvgScore() : BigDecimal.ZERO
        ).reversed());

        for (InspRatingLink link : links) {
            if (!link.isAutoCalculate()) {
                log.debug("Skipping non-auto-calculate link: {}", link.getId());
                continue;
            }

            RatingConfig config = ratingConfigRepository.findById(link.getRatingConfigId()).orElse(null);
            if (config == null || !config.isEnabled()) {
                log.debug("Rating config {} not found or disabled", link.getRatingConfigId());
                continue;
            }

            calculateForConfig(config, ranked, periodStart, periodEnd);
        }
    }

    private void calculateForConfig(RatingConfig config, List<PeriodSummary> ranked,
                                     LocalDate periodStart, LocalDate periodEnd) {
        int totalTargets = ranked.size();
        int awardCount = config.calculateAwardCount(totalTargets);

        log.info("Calculating ratings for config '{}': {} targets, {} awards (method={}, value={})",
                config.getRatingName(), totalTargets, awardCount,
                config.getDivisionMethod(), config.getDivisionValue());

        // Determine which targets to sort (reverse for BOTTOM-based)
        List<PeriodSummary> orderedSummaries;
        if (config.getDivisionMethod().isTopBased()) {
            orderedSummaries = ranked; // Already sorted desc
        } else {
            orderedSummaries = new ArrayList<>(ranked);
            orderedSummaries.sort(Comparator.comparing(
                    (PeriodSummary s) -> s.getAvgScore() != null ? s.getAvgScore() : BigDecimal.ZERO
            ));
        }

        // Map  PeriodType to Rating PeriodType
        RatingPeriodType ratingPeriodType = mapPeriodType(config.getPeriodType());

        List<RatingResult> results = new ArrayList<>();
        for (int i = 0; i < orderedSummaries.size(); i++) {
            PeriodSummary summary = orderedSummaries.get(i);
            boolean awarded = i < awardCount;

            RatingResult result = RatingResult.create(
                    config.getId(),
                    null, // no V6 checkPlanId
                    summary.getTargetId(),
                    summary.getTargetName(),
                    ratingPeriodType,
                    periodStart,
                    periodEnd,
                    summary.getRanking(),
                    summary.getAvgScore(),
                    awarded
            );

            // Auto-submit if approval is required, otherwise stay as DRAFT
            if (config.isRequireApproval()) {
                result.submitForApproval();
            }

            results.add(result);
        }

        // Save all results
        for (RatingResult result : results) {
            ratingResultRepository.save(result);
            // Publish domain events
            result.getDomainEvents().forEach(eventPublisher::publish);
            result.clearDomainEvents();
        }

        log.info("Created {} rating results for config '{}' ({} awarded)",
                results.size(), config.getRatingName(), awardCount);
    }

    private RatingPeriodType mapPeriodType(RatingPeriodType configPeriodType) {
        // Use whatever the config says
        return configPeriodType != null ? configPeriodType : RatingPeriodType.WEEKLY;
    }
}
