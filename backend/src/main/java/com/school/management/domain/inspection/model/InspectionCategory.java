package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * InspectionCategory entity.
 * Represents a category within an inspection template (e.g., "Cleanliness", "Discipline").
 */
public class InspectionCategory extends Entity<Long> {

    private Long id;
    private Long templateId;
    private String categoryCode;
    private String categoryName;
    private Integer baseScore;
    private Integer sortOrder;
    private Boolean isEnabled;

    private List<DeductionItem> deductionItems;

    // For JPA/MyBatis
    protected InspectionCategory() {
        this.deductionItems = new ArrayList<>();
    }

    private InspectionCategory(Builder builder) {
        this.id = builder.id;
        this.templateId = builder.templateId;
        this.categoryCode = builder.categoryCode;
        this.categoryName = builder.categoryName;
        this.baseScore = builder.baseScore != null ? builder.baseScore : 100;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.deductionItems = builder.deductionItems != null
            ? new ArrayList<>(builder.deductionItems)
            : new ArrayList<>();

        validate();
    }

    /**
     * Factory method to create a new category.
     */
    public static InspectionCategory create(Long templateId, String categoryCode,
                                            String categoryName, Integer baseScore,
                                            Integer sortOrder) {
        return builder()
            .templateId(templateId)
            .categoryCode(categoryCode)
            .categoryName(categoryName)
            .baseScore(baseScore)
            .sortOrder(sortOrder)
            .build();
    }

    /**
     * Adds a deduction item to this category.
     */
    public void addDeductionItem(DeductionItem item) {
        this.deductionItems.add(item);
    }

    /**
     * Removes a deduction item from this category.
     */
    public void removeDeductionItem(Long itemId) {
        this.deductionItems.removeIf(item -> item.getId().equals(itemId));
    }

    /**
     * Enables this category.
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * Disables this category.
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * Updates category information.
     */
    public void updateInfo(String categoryName, Integer baseScore, Integer sortOrder) {
        if (categoryName != null && !categoryName.isBlank()) {
            this.categoryName = categoryName;
        }
        if (baseScore != null && baseScore > 0) {
            this.baseScore = baseScore;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * Calculates maximum possible deduction for this category.
     */
    public int calculateMaxDeduction() {
        return deductionItems.stream()
            .mapToInt(DeductionItem::getMaxDeduction)
            .sum();
    }

    private void validate() {
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new IllegalArgumentException("Category code cannot be empty");
        }
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        if (baseScore == null || baseScore <= 0) {
            throw new IllegalArgumentException("Base score must be positive");
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

    public Long getTemplateId() {
        return templateId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Integer getBaseScore() {
        return baseScore;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public Boolean isEnabled() {
        return isEnabled;
    }

    public List<DeductionItem> getDeductionItems() {
        return Collections.unmodifiableList(deductionItems);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long templateId;
        private String categoryCode;
        private String categoryName;
        private Integer baseScore;
        private Integer sortOrder;
        private Boolean isEnabled;
        private List<DeductionItem> deductionItems;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder templateId(Long templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public Builder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public Builder baseScore(Integer baseScore) {
            this.baseScore = baseScore;
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

        public Builder deductionItems(List<DeductionItem> deductionItems) {
            this.deductionItems = deductionItems;
            return this;
        }

        public InspectionCategory build() {
            return new InspectionCategory(this);
        }
    }
}
