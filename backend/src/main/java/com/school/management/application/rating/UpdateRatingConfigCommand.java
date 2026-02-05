package com.school.management.application.rating;

import com.school.management.domain.rating.model.DivisionMethod;
import com.school.management.domain.rating.model.RatingPeriodType;

import java.math.BigDecimal;

/**
 * Command for updating a rating configuration.
 */
public class UpdateRatingConfigCommand {
    private String ratingName;
    private RatingPeriodType periodType;
    private DivisionMethod divisionMethod;
    private BigDecimal divisionValue;
    private String icon;
    private String color;
    private Integer priority;
    private String description;
    private Boolean requireApproval;
    private Boolean autoPublish;

    public UpdateRatingConfigCommand() {}

    // Getters
    public String getRatingName() { return ratingName; }
    public RatingPeriodType getPeriodType() { return periodType; }
    public DivisionMethod getDivisionMethod() { return divisionMethod; }
    public BigDecimal getDivisionValue() { return divisionValue; }
    public String getIcon() { return icon; }
    public String getColor() { return color; }
    public Integer getPriority() { return priority; }
    public String getDescription() { return description; }
    public Boolean getRequireApproval() { return requireApproval; }
    public Boolean getAutoPublish() { return autoPublish; }

    // Setters
    public void setRatingName(String ratingName) { this.ratingName = ratingName; }
    public void setPeriodType(RatingPeriodType periodType) { this.periodType = periodType; }
    public void setDivisionMethod(DivisionMethod divisionMethod) { this.divisionMethod = divisionMethod; }
    public void setDivisionValue(BigDecimal divisionValue) { this.divisionValue = divisionValue; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColor(String color) { this.color = color; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public void setDescription(String description) { this.description = description; }
    public void setRequireApproval(Boolean requireApproval) { this.requireApproval = requireApproval; }
    public void setAutoPublish(Boolean autoPublish) { this.autoPublish = autoPublish; }

    // Builder pattern
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final UpdateRatingConfigCommand instance = new UpdateRatingConfigCommand();

        public Builder ratingName(String ratingName) { instance.ratingName = ratingName; return this; }
        public Builder periodType(RatingPeriodType periodType) { instance.periodType = periodType; return this; }
        public Builder divisionMethod(DivisionMethod divisionMethod) { instance.divisionMethod = divisionMethod; return this; }
        public Builder divisionValue(BigDecimal divisionValue) { instance.divisionValue = divisionValue; return this; }
        public Builder icon(String icon) { instance.icon = icon; return this; }
        public Builder color(String color) { instance.color = color; return this; }
        public Builder priority(Integer priority) { instance.priority = priority; return this; }
        public Builder description(String description) { instance.description = description; return this; }
        public Builder requireApproval(Boolean requireApproval) { instance.requireApproval = requireApproval; return this; }
        public Builder autoPublish(Boolean autoPublish) { instance.autoPublish = autoPublish; return this; }

        public UpdateRatingConfigCommand build() { return instance; }
    }
}
