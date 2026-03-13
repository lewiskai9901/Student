package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.analytics.CorrectiveSummary;
import com.school.management.domain.inspection.model.v7.analytics.PeriodType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CorrectiveSummaryRepository {

    CorrectiveSummary save(CorrectiveSummary summary);

    Optional<CorrectiveSummary> findByProjectAndPeriod(Long projectId, PeriodType periodType, LocalDate periodStart);

    List<CorrectiveSummary> findByProject(Long projectId);

    void deleteById(Long id);
}
