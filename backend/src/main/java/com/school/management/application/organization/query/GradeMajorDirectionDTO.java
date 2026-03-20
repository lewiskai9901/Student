package com.school.management.application.organization.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 年级-专业方向关联 DTO
 */
@Data
public class GradeMajorDirectionDTO {
    private Long id;
    private Integer academicYear;
    private Long majorDirectionId;
    private Long majorId;
    private String majorCode;
    private String directionCode;
    private String directionName;
    private String majorName;
    private String level;
    private Integer years;
    private Long orgUnitId;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private Integer actualClassCount;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
