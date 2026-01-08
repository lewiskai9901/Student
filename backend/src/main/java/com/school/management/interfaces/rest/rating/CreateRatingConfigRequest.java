package com.school.management.interfaces.rest.rating;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request for creating a rating configuration.
 */
@Data
public class CreateRatingConfigRequest {

    @NotNull(message = "Check plan ID is required")
    private Long checkPlanId;

    @NotBlank(message = "Rating name is required")
    private String ratingName;

    private String periodType;

    private String divisionMethod;

    private BigDecimal divisionValue;

    private String icon;

    private String color;

    private Integer priority;

    private String description;

    private boolean requireApproval = true;

    private boolean autoPublish = false;
}
