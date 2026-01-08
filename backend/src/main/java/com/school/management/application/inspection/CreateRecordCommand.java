package com.school.management.application.inspection;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Command to create a new inspection record.
 */
@Data
@Builder
public class CreateRecordCommand {
    private Long templateId;
    private Long roundId;
    private LocalDate inspectionDate;
    private String inspectionPeriod;
    private Long inspectorId;
    private String inspectorName;
    private Long createdBy;
}
