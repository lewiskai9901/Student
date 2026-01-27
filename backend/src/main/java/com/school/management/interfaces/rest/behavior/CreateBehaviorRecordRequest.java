package com.school.management.interfaces.rest.behavior;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBehaviorRecordRequest {
    @NotNull
    private Long studentId;
    @NotNull
    private Long classId;
    @NotNull
    private String behaviorType; // VIOLATION, COMMENDATION
    @NotNull
    private String source; // INSPECTION, TEACHER_REPORT, SELF_REPORT
    private Long sourceId;
    @NotNull
    private String category; // HYGIENE, DISCIPLINE, SAFETY, ATTENDANCE, ACADEMIC, OTHER
    @NotBlank
    private String title;
    private String detail;
    private BigDecimal deductionAmount;
}
