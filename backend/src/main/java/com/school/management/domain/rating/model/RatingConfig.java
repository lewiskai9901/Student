package com.school.management.domain.rating.model;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Rating configuration aggregate root.
 *
 * <p>Defines the criteria and rules for rating classes,
 * such as "Excellent Class" or "Hygiene Model Class".
 */
public class RatingConfig extends AggregateRoot<Long> {

    private Long checkPlanId;
    private String ratingName;
    private RatingPeriodType periodType;
    private String icon;
    private String color;
    private Integer priority;
    private DivisionMethod divisionMethod;
    private BigDecimal divisionValue;
    private boolean requireApproval;
    private boolean autoPublish;
    private boolean enabled;
    private Integer sortOrder;
    private String description;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected RatingConfig() {}

    /**
     * Creates a new rating configuration.
     *
     * @param checkPlanId     the check plan ID
     * @param ratingName      the rating name
     * @param periodType      the period type
     * @param divisionMethod  the division method
     * @param divisionValue   the division value
     * @param createdBy       creator user ID
     * @return new RatingConfig instance
     */
    public static RatingConfig create(Long checkPlanId, String ratingName,
                                       RatingPeriodType periodType,
                                       DivisionMethod divisionMethod,
                                       BigDecimal divisionValue,
                                       Long createdBy) {
        RatingConfig config = new RatingConfig();
        config.checkPlanId = Objects.requireNonNull(checkPlanId, "Check plan ID required");
        config.ratingName = Objects.requireNonNull(ratingName, "Rating name required");
        config.periodType = periodType != null ? periodType : RatingPeriodType.DAILY;
        config.divisionMethod = divisionMethod != null ? divisionMethod : DivisionMethod.TOP_N;
        config.divisionValue = divisionValue != null ? divisionValue : BigDecimal.valueOf(3);
        config.requireApproval = true;
        config.autoPublish = false;
        config.enabled = true;
        config.priority = 0;
        config.sortOrder = 0;
        config.createdBy = createdBy;
        config.createdAt = LocalDateTime.now();
        config.updatedAt = config.createdAt;
        return config;
    }

    /**
     * Updates the rating configuration.
     *
     * @param ratingName     new rating name
     * @param periodType     new period type
     * @param divisionMethod new division method
     * @param divisionValue  new division value
     * @param description    new description
     */
    public void update(String ratingName, RatingPeriodType periodType,
                       DivisionMethod divisionMethod, BigDecimal divisionValue,
                       String description) {
        if (ratingName != null) this.ratingName = ratingName;
        if (periodType != null) this.periodType = periodType;
        if (divisionMethod != null) this.divisionMethod = divisionMethod;
        if (divisionValue != null) this.divisionValue = divisionValue;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Sets the visual appearance.
     *
     * @param icon     icon identifier
     * @param color    hex color code
     * @param priority display priority
     */
    public void setAppearance(String icon, String color, Integer priority) {
        this.icon = icon;
        this.color = color;
        if (priority != null) this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Configures approval workflow.
     *
     * @param requireApproval whether approval is required
     * @param autoPublish     whether to auto-publish after approval
     */
    public void configureApproval(boolean requireApproval, boolean autoPublish) {
        this.requireApproval = requireApproval;
        this.autoPublish = autoPublish;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Enables or disables this configuration.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates how many classes should receive this rating.
     *
     * @param totalClasses total number of classes
     * @return number of classes to receive rating
     */
    public int calculateAwardCount(int totalClasses) {
        if (totalClasses <= 0) return 0;

        if (divisionMethod.isPercentBased()) {
            double percent = divisionValue.doubleValue() / 100.0;
            return (int) Math.ceil(totalClasses * percent);
        } else {
            return Math.min(divisionValue.intValue(), totalClasses);
        }
    }

    // Getters

    public Long getCheckPlanId() {
        return checkPlanId;
    }

    public String getRatingName() {
        return ratingName;
    }

    public RatingPeriodType getPeriodType() {
        return periodType;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public Integer getPriority() {
        return priority;
    }

    public DivisionMethod getDivisionMethod() {
        return divisionMethod;
    }

    public BigDecimal getDivisionValue() {
        return divisionValue;
    }

    public boolean isRequireApproval() {
        return requireApproval;
    }

    public boolean isAutoPublish() {
        return autoPublish;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Builder for reconstruction

    public static class Builder {
        private final RatingConfig config = new RatingConfig();

        public Builder id(Long id) { config.setId(id); return this; }
        public Builder checkPlanId(Long v) { config.checkPlanId = v; return this; }
        public Builder ratingName(String v) { config.ratingName = v; return this; }
        public Builder periodType(RatingPeriodType v) { config.periodType = v; return this; }
        public Builder icon(String v) { config.icon = v; return this; }
        public Builder color(String v) { config.color = v; return this; }
        public Builder priority(Integer v) { config.priority = v; return this; }
        public Builder divisionMethod(DivisionMethod v) { config.divisionMethod = v; return this; }
        public Builder divisionValue(BigDecimal v) { config.divisionValue = v; return this; }
        public Builder requireApproval(boolean v) { config.requireApproval = v; return this; }
        public Builder autoPublish(boolean v) { config.autoPublish = v; return this; }
        public Builder enabled(boolean v) { config.enabled = v; return this; }
        public Builder sortOrder(Integer v) { config.sortOrder = v; return this; }
        public Builder description(String v) { config.description = v; return this; }
        public Builder createdBy(Long v) { config.createdBy = v; return this; }
        public Builder createdAt(LocalDateTime v) { config.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { config.updatedAt = v; return this; }
        public Builder version(Long v) { config.setVersion(v); return this; }
        public RatingConfig build() { return config; }
    }

    public static Builder builder() { return new Builder(); }
}
