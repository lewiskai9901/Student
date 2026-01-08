package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;

/**
 * DeductionItem entity.
 * Represents a deduction rule within a category.
 */
public class DeductionItem extends Entity<Long> {

    private Long id;
    private Long categoryId;
    private String itemCode;
    private String itemName;
    private String description;
    private DeductionMode deductionMode;
    private BigDecimal fixedDeduction;
    private BigDecimal perPersonDeduction;
    private BigDecimal minDeduction;
    private BigDecimal maxDeduction;
    private BigDecimal stepValue;
    private Integer sortOrder;
    private Boolean isEnabled;

    // For JPA/MyBatis
    protected DeductionItem() {
    }

    private DeductionItem(Builder builder) {
        this.id = builder.id;
        this.categoryId = builder.categoryId;
        this.itemCode = builder.itemCode;
        this.itemName = builder.itemName;
        this.description = builder.description;
        this.deductionMode = builder.deductionMode != null
            ? builder.deductionMode
            : DeductionMode.FIXED_DEDUCT;
        this.fixedDeduction = builder.fixedDeduction;
        this.perPersonDeduction = builder.perPersonDeduction;
        this.minDeduction = builder.minDeduction;
        this.maxDeduction = builder.maxDeduction;
        this.stepValue = builder.stepValue;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;

        validate();
    }

    /**
     * Factory method for fixed deduction item.
     */
    public static DeductionItem createFixed(Long categoryId, String itemCode,
                                            String itemName, BigDecimal deduction) {
        return builder()
            .categoryId(categoryId)
            .itemCode(itemCode)
            .itemName(itemName)
            .deductionMode(DeductionMode.FIXED_DEDUCT)
            .fixedDeduction(deduction)
            .build();
    }

    /**
     * Factory method for per-person deduction item.
     */
    public static DeductionItem createPerPerson(Long categoryId, String itemCode,
                                                String itemName, BigDecimal perPersonDeduction,
                                                BigDecimal maxDeduction) {
        return builder()
            .categoryId(categoryId)
            .itemCode(itemCode)
            .itemName(itemName)
            .deductionMode(DeductionMode.PER_PERSON_DEDUCT)
            .perPersonDeduction(perPersonDeduction)
            .maxDeduction(maxDeduction)
            .build();
    }

    /**
     * Factory method for range-based deduction item.
     */
    public static DeductionItem createRange(Long categoryId, String itemCode,
                                            String itemName, BigDecimal minDeduction,
                                            BigDecimal maxDeduction, BigDecimal stepValue) {
        return builder()
            .categoryId(categoryId)
            .itemCode(itemCode)
            .itemName(itemName)
            .deductionMode(DeductionMode.SCORE_RANGE)
            .minDeduction(minDeduction)
            .maxDeduction(maxDeduction)
            .stepValue(stepValue)
            .build();
    }

    /**
     * Calculates the deduction amount based on the mode and count.
     */
    public BigDecimal calculateDeduction(int count) {
        switch (deductionMode) {
            case FIXED_DEDUCT:
                return count > 0 ? fixedDeduction : BigDecimal.ZERO;

            case PER_PERSON_DEDUCT:
                BigDecimal total = perPersonDeduction.multiply(BigDecimal.valueOf(count));
                if (maxDeduction != null && total.compareTo(maxDeduction) > 0) {
                    return maxDeduction;
                }
                return total;

            case SCORE_RANGE:
                // For range mode, count represents the selected score level
                BigDecimal rangeValue = minDeduction.add(
                    stepValue.multiply(BigDecimal.valueOf(count)));
                if (rangeValue.compareTo(maxDeduction) > 0) {
                    return maxDeduction;
                }
                return rangeValue;

            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Returns the maximum possible deduction for this item.
     */
    public int getMaxDeduction() {
        switch (deductionMode) {
            case FIXED_DEDUCT:
                return fixedDeduction != null ? fixedDeduction.intValue() : 0;
            case PER_PERSON_DEDUCT:
            case SCORE_RANGE:
                return maxDeduction != null ? maxDeduction.intValue() : 0;
            default:
                return 0;
        }
    }

    private void validate() {
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("Item code cannot be empty");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (deductionMode == null) {
            throw new IllegalArgumentException("Deduction mode is required");
        }

        switch (deductionMode) {
            case FIXED_DEDUCT:
                if (fixedDeduction == null || fixedDeduction.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Fixed deduction must be positive");
                }
                break;
            case PER_PERSON_DEDUCT:
                if (perPersonDeduction == null || perPersonDeduction.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Per-person deduction must be positive");
                }
                break;
            case SCORE_RANGE:
                if (minDeduction == null || maxDeduction == null) {
                    throw new IllegalArgumentException("Min and max deduction are required for range mode");
                }
                if (minDeduction.compareTo(maxDeduction) > 0) {
                    throw new IllegalArgumentException("Min deduction cannot exceed max deduction");
                }
                break;
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public DeductionMode getDeductionMode() {
        return deductionMode;
    }

    public BigDecimal getFixedDeduction() {
        return fixedDeduction;
    }

    public BigDecimal getPerPersonDeduction() {
        return perPersonDeduction;
    }

    public BigDecimal getMinDeduction() {
        return minDeduction;
    }

    public BigDecimal getMaxDeductionValue() {
        return maxDeduction;
    }

    public BigDecimal getStepValue() {
        return stepValue;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long categoryId;
        private String itemCode;
        private String itemName;
        private String description;
        private DeductionMode deductionMode;
        private BigDecimal fixedDeduction;
        private BigDecimal perPersonDeduction;
        private BigDecimal minDeduction;
        private BigDecimal maxDeduction;
        private BigDecimal stepValue;
        private Integer sortOrder;
        private Boolean isEnabled;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder itemCode(String itemCode) {
            this.itemCode = itemCode;
            return this;
        }

        public Builder itemName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder deductionMode(DeductionMode deductionMode) {
            this.deductionMode = deductionMode;
            return this;
        }

        public Builder fixedDeduction(BigDecimal fixedDeduction) {
            this.fixedDeduction = fixedDeduction;
            return this;
        }

        public Builder perPersonDeduction(BigDecimal perPersonDeduction) {
            this.perPersonDeduction = perPersonDeduction;
            return this;
        }

        public Builder minDeduction(BigDecimal minDeduction) {
            this.minDeduction = minDeduction;
            return this;
        }

        public Builder maxDeduction(BigDecimal maxDeduction) {
            this.maxDeduction = maxDeduction;
            return this;
        }

        public Builder stepValue(BigDecimal stepValue) {
            this.stepValue = stepValue;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder isEnabled(Boolean isEnabled) {
            this.isEnabled = isEnabled;
            return this;
        }

        public DeductionItem build() {
            return new DeductionItem(this);
        }
    }
}
