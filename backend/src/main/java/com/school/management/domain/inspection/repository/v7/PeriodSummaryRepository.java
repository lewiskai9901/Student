package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.analytics.PeriodSummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodSummaryRepository {

    PeriodSummary save(PeriodSummary summary);

    Optional<PeriodSummary> findById(Long id);

    Optional<PeriodSummary> findByProjectPeriodTarget(Long projectId, PeriodType periodType,
                                                        LocalDate periodStart, String targetType, Long targetId);

    List<PeriodSummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart);

    List<PeriodSummary> findByTarget(String targetType, Long targetId, PeriodType periodType);

    List<PeriodSummary> findRanking(Long projectId, PeriodType periodType, LocalDate periodStart);

    void deleteByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart);
}
