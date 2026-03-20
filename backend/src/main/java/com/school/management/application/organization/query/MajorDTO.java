package com.school.management.application.organization.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业 DTO
 */
@Data
public class MajorDTO {
    private Long id;
    private String majorCode;
    private String majorName;
    private Long orgUnitId;
    private String orgUnitName;
    private String description;
    private Integer status;
    private String statusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
