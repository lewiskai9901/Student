package com.school.management.application.academic.query;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培养方案 DTO
 */
@Data
public class CurriculumPlanDTO {
    private Long id;
    private String planCode;
    private String planName;
    private Long majorId;
    private Integer gradeYear;
    private BigDecimal totalCredits;
    private BigDecimal requiredCredits;
    private BigDecimal electiveCredits;
    private BigDecimal practiceCredits;
    private String trainingObjective;
    private String graduationRequirement;
    private Integer version;
    private Integer status;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
