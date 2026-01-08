package com.school.management.application.rating;

import com.school.management.domain.rating.model.DivisionMethod;
import com.school.management.domain.rating.model.RatingPeriodType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Command for creating a rating configuration.
 */
@Data
@Builder
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
}
