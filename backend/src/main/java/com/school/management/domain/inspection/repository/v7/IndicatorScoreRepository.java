package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.IndicatorScore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IndicatorScoreRepository {

    IndicatorScore save(IndicatorScore score);

    Optional<IndicatorScore> findByIndicatorAndTargetAndPeriod(Long indicatorId, Long targetId, LocalDate periodStart);

    List<IndicatorScore> findByIndicatorId(Long indicatorId);

    List<IndicatorScore> findByIndicatorIdAndPeriod(Long indicatorId, LocalDate periodStart, LocalDate periodEnd);

    void deleteByIndicatorId(Long indicatorId);
}
