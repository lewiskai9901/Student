package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.RatingResult;

import java.time.LocalDate;
import java.util.List;

public interface RatingResultRepository {

    RatingResult save(RatingResult result);

    List<RatingResult> findByDimensionId(Long dimensionId);

    List<RatingResult> findByDimensionIdAndCycleDate(Long dimensionId, LocalDate cycleDate);

    void deleteByDimensionId(Long dimensionId);

    void deleteByDimensionIdAndCycleDate(Long dimensionId, LocalDate cycleDate);
}
