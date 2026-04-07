package com.school.management.interfaces.rest.rating;

import com.school.management.domain.rating.model.RatingPeriodType;
import com.school.management.domain.rating.model.RatingResultStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Rating result response DTO.
 */
@Data
public class RatingResultResponse {
    private Long id;
    private Long ratingConfigId;
    private Long checkPlanId;
    private Long orgUnitId;
    private String className;
    private RatingPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer ranking;
    private BigDecimal finalScore;
    private boolean awarded;
    private RatingResultStatus status;
    private LocalDateTime calculatedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private List<String> allowedTransitions;
}
