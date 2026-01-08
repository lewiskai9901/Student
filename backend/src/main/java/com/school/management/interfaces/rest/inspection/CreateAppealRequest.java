package com.school.management.interfaces.rest.inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating an appeal.
 */
@Data
@Schema(description = "Create appeal request")
public class CreateAppealRequest {

    @Schema(description = "Inspection record ID")
    @NotNull(message = "Inspection record ID is required")
    private Long inspectionRecordId;

    @Schema(description = "Deduction detail ID")
    @NotNull(message = "Deduction detail ID is required")
    private Long deductionDetailId;

    @Schema(description = "Class ID")
    @NotNull(message = "Class ID is required")
    private Long classId;

    @Schema(description = "Appeal reason")
    @NotBlank(message = "Reason is required")
    private String reason;

    @Schema(description = "Evidence attachment URLs")
    private List<String> attachments;

    @Schema(description = "Original deduction amount")
    @NotNull(message = "Original deduction is required")
    private BigDecimal originalDeduction;

    @Schema(description = "Requested deduction amount (after appeal)")
    @NotNull(message = "Requested deduction is required")
    private BigDecimal requestedDeduction;
}
