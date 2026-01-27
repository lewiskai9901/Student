package com.school.management.interfaces.rest.inspection.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSessionRequest {

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotNull(message = "Inspection date is required")
    private LocalDate inspectionDate;

    private String inspectionPeriod;
    private String inputMode;
    private String scoringMode;
    private Integer baseScore;
    private String inspectionLevel;
}
