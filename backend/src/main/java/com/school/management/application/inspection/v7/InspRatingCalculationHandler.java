package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.event.v7.PeriodSummaryCalculatedEvent;
import com.school.management.domain.inspection.model.v7.analytics.PeriodSummary;
import com.school.management.domain.inspection.model.v7.rating.InspRatingLink;
import com.school.management.domain.inspection.repository.v7.InspRatingLinkRepository;
import com.school.management.domain.inspection.repository.v7.PeriodSummaryRepository;
import com.school.management.domain.rating.model.*;
import com.school.management.domain.rating.repository.RatingConfigRepository;
import com.school.management.domain.rating.repository.RatingResultRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * V7 检查平台 - 评级计算处理器
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

    @Async
    @EventListener
    public void onPeriodSummaryCalculated(PeriodSummaryCalculatedEvent event) {
        log.info("Rating calculation triggered: projectId={}, periodType={}, period={}-{}",
                event.getProjectId(), event.getPeriodType(), event.getPeriodStart(), event.getPeriodEnd());

        try {
            calculateRatings(event.getProjectId(), event.getPeriodType(),
                    event.getPeriodStart(), event.getPeriodEnd());
        } catch (Exception e) {
            log.error("Failed to calculate ratings for project {}: {}",
                    event.getProjectId(), e.getMessage(), e);
        }
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
        var v7PeriodType = com.school.management.domain.inspection.model.v7.analytics.PeriodType.valueOf(periodType);
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

        // Map V7 PeriodType to Rating PeriodType
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
