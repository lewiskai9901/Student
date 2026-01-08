package com.school.management.interfaces.rest.inspection;

import com.school.management.domain.inspection.model.RecordStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for inspection records.
 */
@Data
@Schema(description = "Record response")
public class RecordResponse {

    @Schema(description = "Record ID")
    private Long id;

    @Schema(description = "Record code")
    private String recordCode;

    @Schema(description = "Template ID")
    private Long templateId;

    @Schema(description = "Template version used")
    private Integer templateVersion;

    @Schema(description = "Round ID")
    private Long roundId;

    @Schema(description = "Inspection date")
    private LocalDate inspectionDate;

    @Schema(description = "Inspection period")
    private String inspectionPeriod;

    @Schema(description = "Record status")
    private RecordStatus status;

    @Schema(description = "Inspector ID")
    private Long inspectorId;

    @Schema(description = "Inspector name")
    private String inspectorName;

    @Schema(description = "Inspected timestamp")
    private LocalDateTime inspectedAt;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Reviewed timestamp")
    private LocalDateTime reviewedAt;

    @Schema(description = "Published timestamp")
    private LocalDateTime publishedAt;

    @Schema(description = "Remarks")
    private String remarks;

    @Schema(description = "Created timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Number of classes inspected")
    private Integer classCount;

    @Schema(description = "Total number of deductions")
    private Integer totalDeductionCount;

    @Schema(description = "Average score across all classes")
    private Double averageScore;
}
