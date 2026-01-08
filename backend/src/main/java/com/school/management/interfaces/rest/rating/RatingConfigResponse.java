package com.school.management.interfaces.rest.rating;

import com.school.management.domain.rating.model.DivisionMethod;
import com.school.management.domain.rating.model.RatingPeriodType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rating configuration response DTO.
 */
@Data
public class RatingConfigResponse {
    private Long id;
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
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
