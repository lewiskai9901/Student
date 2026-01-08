package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ClassScore entity.
 * Represents a class's score within an inspection record.
 */
public class ClassScore extends Entity<Long> {

    private Long id;
    private Long recordId;
    private Long classId;
    private String className;
    private BigDecimal baseScore;
    private BigDecimal totalDeduction;
    private BigDecimal bonusPoints;
    private BigDecimal finalScore;
    private BigDecimal weightedScore;
    private Integer ranking;
    private String ratingLevel;

    private List<DeductionDetail> deductionDetails;

    // For JPA/MyBatis
    protected ClassScore() {
        this.deductionDetails = new ArrayList<>();
    }

    private ClassScore(Builder builder) {
        this.id = builder.id;
        this.recordId = builder.recordId;
        this.classId = builder.classId;
        this.className = builder.className;
        this.baseScore = builder.baseScore != null ? builder.baseScore : new BigDecimal("100");
        this.totalDeduction = BigDecimal.ZERO;
        this.bonusPoints = BigDecimal.ZERO;
        this.finalScore = this.baseScore;
        this.deductionDetails = new ArrayList<>();
    }

    /**
     * Factory method to create a class score.
     */
    public static ClassScore create(Long recordId, Long classId, String className, Integer baseScore) {
        return builder()
            .recordId(recordId)
            .classId(classId)
            .className(className)
            .baseScore(new BigDecimal(baseScore))
            .build();
    }

    /**
     * Adds a deduction to this class score.
     */
    public void addDeduction(Long deductionItemId, String itemName, int count,
                             BigDecimal deductionAmount, String remark,
                             List<String> evidenceUrls) {
        DeductionDetail detail = DeductionDetail.builder()
            .classScoreId(this.id)
            .deductionItemId(deductionItemId)
            .itemName(itemName)
            .count(count)
            .deductionAmount(deductionAmount)
            .remark(remark)
            .evidenceUrls(evidenceUrls)
            .build();

        this.deductionDetails.add(detail);
        recalculateTotalDeduction();
    }

    /**
     * Removes a deduction from this class score.
     */
    public void removeDeduction(Long detailId) {
        this.deductionDetails.removeIf(d -> d.getId().equals(detailId));
        recalculateTotalDeduction();
    }

    /**
     * Adds bonus points to this class.
     */
    public void addBonus(BigDecimal bonus) {
        this.bonusPoints = this.bonusPoints.add(bonus);
        calculateFinalScore();
    }

    /**
     * Recalculates the total deduction from all details.
     */
    private void recalculateTotalDeduction() {
        this.totalDeduction = deductionDetails.stream()
            .map(DeductionDetail::getDeductionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        calculateFinalScore();
    }

    /**
     * Calculates the final score.
     */
    public void calculateFinalScore() {
        this.finalScore = baseScore
            .subtract(totalDeduction)
            .add(bonusPoints);

        // Ensure score doesn't go below 0
        if (this.finalScore.compareTo(BigDecimal.ZERO) < 0) {
            this.finalScore = BigDecimal.ZERO;
        }
    }

    /**
     * Sets the weighted score (after applying weight factors).
     */
    public void setWeightedScore(BigDecimal weightedScore) {
        this.weightedScore = weightedScore;
    }

    /**
     * Sets the ranking among all classes.
     */
    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    /**
     * Sets the rating level (e.g., "A", "B", "C").
     */
    public void setRatingLevel(String ratingLevel) {
        this.ratingLevel = ratingLevel;
    }

    /**
     * Gets the deduction count for this class.
     */
    public int getDeductionCount() {
        return deductionDetails.size();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public Long getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public BigDecimal getBaseScore() {
        return baseScore;
    }

    public BigDecimal getTotalDeduction() {
        return totalDeduction;
    }

    public BigDecimal getBonusPoints() {
        return bonusPoints;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public BigDecimal getWeightedScore() {
        return weightedScore;
    }

    public Integer getRanking() {
        return ranking;
    }

    public String getRatingLevel() {
        return ratingLevel;
    }

    public List<DeductionDetail> getDeductionDetails() {
        return Collections.unmodifiableList(deductionDetails);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long recordId;
        private Long classId;
        private String className;
        private BigDecimal baseScore;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder recordId(Long recordId) {
            this.recordId = recordId;
            return this;
        }

        public Builder classId(Long classId) {
            this.classId = classId;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder baseScore(BigDecimal baseScore) {
            this.baseScore = baseScore;
            return this;
        }

        public ClassScore build() {
            return new ClassScore(this);
        }
    }
}
