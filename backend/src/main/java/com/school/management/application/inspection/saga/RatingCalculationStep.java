package com.school.management.application.inspection.saga;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Saga Step: Rating Calculation (评级计算).
 *
 * Computes class-level ratings (EXCELLENT / GOOD / PASS / FAIL) based on
 * the final inspection scores within a published session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingCalculationStep {

    private static final BigDecimal EXCELLENT_THRESHOLD = new BigDecimal("90");
    private static final BigDecimal GOOD_THRESHOLD = new BigDecimal("80");
    private static final BigDecimal PASS_THRESHOLD = new BigDecimal("60");

    /**
     * Executes rating calculation for all class records in a session.
     *
     * @param sessionId      the published session ID
     * @param inspectionDate the inspection date
     * @param classRecords   all class inspection records in the session
     * @return rating calculation result with distribution and statistics
     */
    public RatingCalculationResult execute(Long sessionId, LocalDate inspectionDate,
                                           List<ClassInspectionRecord> classRecords) {
        log.debug("Starting rating calculation: sessionId={}, classCount={}", sessionId, classRecords.size());

        Map<String, List<Long>> ratingMap = new LinkedHashMap<>();
        ratingMap.put("EXCELLENT", new ArrayList<>());
        ratingMap.put("GOOD", new ArrayList<>());
        ratingMap.put("PASS", new ArrayList<>());
        ratingMap.put("FAIL", new ArrayList<>());

        BigDecimal totalScore = BigDecimal.ZERO;

        for (ClassInspectionRecord record : classRecords) {
            BigDecimal score = resolveScore(record);
            totalScore = totalScore.add(score);

            String rating = calculateRating(score);
            ratingMap.get(rating).add(record.getClassId());
        }

        BigDecimal averageScore = classRecords.isEmpty()
                ? BigDecimal.ZERO
                : totalScore.divide(new BigDecimal(classRecords.size()), 2, RoundingMode.HALF_UP);

        RatingCalculationResult result = new RatingCalculationResult();
        result.setSessionId(sessionId);
        result.setInspectionDate(inspectionDate);
        result.setRatedClassCount(classRecords.size());
        result.setAverageScore(averageScore);
        result.setRatingDistribution(ratingMap);

        log.info("Rating result: EXCELLENT={}, GOOD={}, PASS={}, FAIL={}, averageScore={}",
                ratingMap.get("EXCELLENT").size(),
                ratingMap.get("GOOD").size(),
                ratingMap.get("PASS").size(),
                ratingMap.get("FAIL").size(),
                averageScore);

        return result;
    }

    /**
     * Resolves the effective score for a class record.
     * Prefers the pre-calculated finalScore; falls back to computing from base/deduction/bonus.
     */
    private BigDecimal resolveScore(ClassInspectionRecord record) {
        if (record.getFinalScore() != null && record.getFinalScore().compareTo(BigDecimal.ZERO) != 0) {
            return record.getFinalScore();
        }
        // Fallback: baseScore (Integer) - totalDeduction + bonusScore
        BigDecimal base = record.getBaseScore() != null
                ? new BigDecimal(record.getBaseScore())
                : new BigDecimal("100");
        BigDecimal deduction = record.getTotalDeduction() != null
                ? record.getTotalDeduction()
                : BigDecimal.ZERO;
        BigDecimal bonus = record.getBonusScore() != null
                ? record.getBonusScore()
                : BigDecimal.ZERO;
        return base.subtract(deduction).add(bonus);
    }

    private String calculateRating(BigDecimal score) {
        if (score.compareTo(EXCELLENT_THRESHOLD) >= 0) return "EXCELLENT";
        if (score.compareTo(GOOD_THRESHOLD) >= 0) return "GOOD";
        if (score.compareTo(PASS_THRESHOLD) >= 0) return "PASS";
        return "FAIL";
    }
}
