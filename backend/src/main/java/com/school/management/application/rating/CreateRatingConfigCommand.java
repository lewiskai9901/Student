package com.school.management.application.rating;

import com.school.management.domain.rating.model.DivisionMethod;
import com.school.management.domain.rating.model.RatingPeriodType;

import java.math.BigDecimal;

/**
 * Command for creating a rating configuration.
 */
public class CreateRatingConfigCommand {
    private Long checkPlanId;
    private String ratingName;
    private RatingPeriodType periodType;
    private DivisionMethod divisionMethod;
    private BigDecimal divisionValue;
    private String icon;
    private String color;
    private Integer priority;
    private String description;
    private boolean requireApproval;
    private boolean autoPublish;
    private Long createdBy;

    public CreateRatingConfigCommand() {}

    // Getters
    public Long getCheckPlanId() { return checkPlanId; }
    public String getRatingName() { return ratingName; }
    public RatingPeriodType getPeriodType() { return periodType; }
    public DivisionMethod getDivisionMethod() { return divisionMethod; }
    public BigDecimal getDivisionValue() { return divisionValue; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public Integer getPriority() { return priority; }
    public String getDescription() { return description; }
    public boolean isRequireApproval() { return requireApproval; }
    public boolean isAutoPublish() { return autoPublish; }
    public Long getCreatedBy() { return createdBy; }

    // Setters
    public void setCheckPlanId(Long checkPlanId) { this.checkPlanId = checkPlanId; }
    public void setRatingName(String ratingName) { this.ratingName = ratingName; }
    public void setPeriodType(RatingPeriodType periodType) { this.periodType = periodType; }
    public void setDivisionMethod(DivisionMethod divisionMethod) { this.divisionMethod = divisionMethod; }
    public void setDivisionValue(BigDecimal divisionValue) { this.divisionValue = divisionValue; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColor(String color) { this.color = color; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public void setDescription(String description) { this.description = description; }
    public void setRequireApproval(boolean requireApproval) { this.requireApproval = requireApproval; }
    public void setAutoPublish(boolean autoPublish) { this.autoPublish = autoPublish; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final CreateRatingConfigCommand cmd = new CreateRatingConfigCommand();
        public Builder checkPlanId(Long v) { cmd.checkPlanId = v; return this; }
        public Builder ratingName(String v) { cmd.ratingName = v; return this; }
        public Builder periodType(RatingPeriodType v) { cmd.periodType = v; return this; }
        public Builder divisionMethod(DivisionMethod v) { cmd.divisionMethod = v; return this; }
        public Builder divisionValue(BigDecimal v) { cmd.divisionValue = v; return this; }
        public Builder icon(String v) { cmd.icon = v; return this; }
        public Builder color(String v) { cmd.color = v; return this; }
        public Builder priority(Integer v) { cmd.priority = v; return this; }
        public Builder description(String v) { cmd.description = v; return this; }
        public Builder requireApproval(boolean v) { cmd.requireApproval = v; return this; }
        public Builder autoPublish(boolean v) { cmd.autoPublish = v; return this; }
        public Builder createdBy(Long v) { cmd.createdBy = v; return this; }
        public CreateRatingConfigCommand build() { return cmd; }
    }
}
