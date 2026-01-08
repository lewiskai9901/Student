package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Appeal Review Request")
public class AppealReviewRequest {

    @Schema(description = "Appeal ID")
    private Long appealId;

    @NotNull(message = "Status cannot be null")
    @Schema(description = "Review Status 2-Approved 3-Rejected", required = true)
    private Integer status;

    @Schema(description = "Review Comment")
    private String reviewComment;

    @Schema(description = "Adjusted Score Required when approved")
    private BigDecimal adjustedScore;

    @Schema(description = "Revised Score after review")
    private BigDecimal revisedScore;

    @Schema(description = "Review Opinion")
    private String reviewOpinion;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Reviewer Name")
    private String reviewerName;
}
