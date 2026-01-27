package com.school.management.application.inspection.command;

import com.school.management.domain.inspection.model.InputMode;
import com.school.management.domain.inspection.model.ScoringMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CreateSessionCommand {
    private Long templateId;
    private LocalDate inspectionDate;
    private String inspectionPeriod;
    private InputMode inputMode;
    private ScoringMode scoringMode;
    private Integer baseScore;
    private Long inspectorId;
    private String inspectorName;
    private Long createdBy;
}
