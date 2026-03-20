package com.school.management.application.organization.query;

import lombok.Data;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
