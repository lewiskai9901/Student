package com.school.management.application.academic.query;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 专业方向 DTO
 */
@Data
public class MajorDirectionDTO {
    private Long id;
    private Long majorId;
    private String directionCode;
    private String directionName;
    private String level;
    private Integer years;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private String remarks;
    private String majorName;
    private String levelDisplay;
    private String yearsDisplay;
    private String enrollmentTarget;
    private String educationForm;
    private List<String> certificateNames;
    private String trainingStandard;
    private String cooperationEnterprise;
    private Integer maxEnrollment;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
