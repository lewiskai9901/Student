package com.school.management.interfaces.rest.inspection;

import com.school.management.domain.inspection.model.AppealStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for appeal data.
 */
@Data
@Schema(description = "Appeal response")
public class AppealResponse {

    @Schema(description = "Appeal ID")
    private Long id;

    @Schema(description = "Appeal code")
    private String appealCode;

    @Schema(description = "Inspection record ID")
    private Long inspectionRecordId;

    @Schema(description = "Deduction detail ID")
    private Long deductionDetailId;

    @Schema(description = "Class ID")
    private Long classId;

    @Schema(description = "Appeal reason")
    private String reason;

    @Schema(description = "Evidence attachments")
    private List<String> attachments;

    @Schema(description = "Original deduction amount")
    private BigDecimal originalDeduction;

    @Schema(description = "Requested deduction amount")
    private BigDecimal requestedDeduction;

    @Schema(description = "Approved deduction amount")
    private BigDecimal approvedDeduction;

    @Schema(description = "Current status")
    private AppealStatus status;

    @Schema(description = "Applicant ID")
    private Long applicantId;

    @Schema(description = "Applied timestamp")
    private LocalDateTime appliedAt;

    @Schema(description = "Level 1 reviewer ID")
    private Long level1ReviewerId;

    @Schema(description = "Level 1 reviewed timestamp")
    private LocalDateTime level1ReviewedAt;

    @Schema(description = "Level 1 review comment")
    private String level1Comment;

    @Schema(description = "Level 2 reviewer ID")
    private Long level2ReviewerId;

    @Schema(description = "Level 2 reviewed timestamp")
    private LocalDateTime level2ReviewedAt;

    @Schema(description = "Level 2 review comment")
    private String level2Comment;

    @Schema(description = "Effective timestamp")
    private LocalDateTime effectiveAt;

    @Schema(description = "Allowed status transitions from current status")
    private List<String> allowedTransitions;
}
