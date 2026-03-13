package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.analytics.ItemFrequencySummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;

import java.time.LocalDate;
import java.util.List;

public interface ItemFrequencySummaryRepository {

    ItemFrequencySummary save(ItemFrequencySummary summary);

    List<ItemFrequencySummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart);

    List<ItemFrequencySummary> findTopNByDeduction(Long projectId, PeriodType periodType,
                                                    LocalDate periodStart, int limit);

    void deleteById(Long id);
}
