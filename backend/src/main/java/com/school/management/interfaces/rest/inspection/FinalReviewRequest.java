package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO for final appeal review (approval with adjusted amount).
 */
@Data
@Schema(description = "Final review request")
public class FinalReviewRequest {

    @Schema(description = "Review comment")
    private String comment;

    @Schema(description = "Approved deduction amount")
    @NotNull(message = "Approved deduction is required")
    private BigDecimal approvedDeduction;
}
