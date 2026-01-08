package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Appeal Information")
public class AppealDTO {

    @Schema(description = "Appeal ID")
    private Long id;

    @Schema(description = "Appeal Code")
    private String appealCode;

    @Schema(description = "Check Record ID")
    private Long recordId;

    @Schema(description = "Check Name")
    private String checkName;

    @Schema(description = "Check Date")
    private String checkDate;

    @Schema(description = "Class ID")
    private Long classId;

    @Schema(description = "Class Name")
    private String className;

    @Schema(description = "Deduction Item Description")
    private String itemDescription;

    @Schema(description = "Original Score")
    private BigDecimal originalScore;

    @Schema(description = "Appeal Reason")
    private String appealReason;

    @Schema(description = "Evidence URLs")
    private List<String> evidenceUrls;

    @Schema(description = "Appellant ID")
    private Long appellantId;

    @Schema(description = "Appellant Name")
    private String appellantName;

    @Schema(description = "Status 1-Pending 2-Approved 3-Rejected 4-Cancelled")
    private Integer status;

    @Schema(description = "Status Text")
    private String statusText;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Reviewer Name")
    private String reviewerName;

    @Schema(description = "Review Time")
    private LocalDateTime reviewTime;

    @Schema(description = "Review Comment")
    private String reviewComment;

    @Schema(description = "Adjusted Score")
    private BigDecimal adjustedScore;

    @Schema(description = "Created At")
    private LocalDateTime createdAt;

    @Schema(description = "Updated At")
    private LocalDateTime updatedAt;
}
