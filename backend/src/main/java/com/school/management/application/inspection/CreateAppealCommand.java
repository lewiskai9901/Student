package com.school.management.application.inspection;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Command to create a new appeal.
 */
@Data
@Builder
public class CreateAppealCommand {
    private Long inspectionRecordId;
    private Long deductionDetailId;
    private Long classId;
    private String reason;
    private List<String> attachments;
    private BigDecimal originalDeduction;
    private BigDecimal requestedDeduction;
    private Long applicantId;
}
