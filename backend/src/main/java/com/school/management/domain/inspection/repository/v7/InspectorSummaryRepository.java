package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.analytics.InspectorSummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InspectorSummaryRepository {

    InspectorSummary save(InspectorSummary summary);

    Optional<InspectorSummary> findById(Long id);

    List<InspectorSummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart);

    Optional<InspectorSummary> findByInspectorAndPeriod(Long projectId, Long inspectorId,
                                                         PeriodType periodType, LocalDate periodStart);

    void deleteById(Long id);
}
