package com.school.management.application.inspection.saga;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Result of the rating calculation step in the InspectionCompletionSaga.
 * Contains aggregated scoring statistics and per-class rating distribution.
 */
@Data
public class RatingCalculationResult {

    private Long sessionId;
    private LocalDate inspectionDate;
    private int ratedClassCount;
    private BigDecimal averageScore;

    /**
     * Rating distribution: key is rating level (EXCELLENT/GOOD/PASS/FAIL),
     * value is list of classIds that received that rating.
     */
    private Map<String, List<Long>> ratingDistribution;
}
