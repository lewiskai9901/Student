package com.school.management.interfaces.rest.rating;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request for updating a rating configuration.
 */
@Data
public class UpdateRatingConfigRequest {
    private String ratingName;
    private String periodType;
    private String divisionMethod;
    private BigDecimal divisionValue;
    private String icon;
    private String color;
    private Integer priority;
    private String description;
    private Boolean requireApproval;
    private Boolean autoPublish;
}
