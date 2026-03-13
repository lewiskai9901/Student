package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.analytics.DailySummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryRepository {

    DailySummary save(DailySummary summary);

    Optional<DailySummary> findById(Long id);

    Optional<DailySummary> findByProjectDateTarget(Long projectId, LocalDate summaryDate,
                                                     String targetType, Long targetId);

    List<DailySummary> findByProjectAndDate(Long projectId, LocalDate summaryDate);

    List<DailySummary> findByProjectAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate);

    List<DailySummary> findByTarget(String targetType, Long targetId, LocalDate startDate, LocalDate endDate);

    List<DailySummary> findRanking(Long projectId, LocalDate summaryDate);

    void deleteByProjectAndDate(Long projectId, LocalDate summaryDate);
}
