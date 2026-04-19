package com.school.management.interfaces.rest.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Update class request
 */
@Schema(description = "Update class request")
public class UpdateClassRequest {

    @Schema(description = "Class name", example = "2024 Computer Science Class 1")
    private String className;

    @Schema(description = "Short name", example = "CS-1")
    private String shortName;

    @Schema(description = "Standard class size", example = "50")
    private Integer standardSize;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;

    @Schema(description = "Organization unit ID")
    private Long orgUnitId;

    @Schema(description = "Grade ID")
    private Long gradeId;

    @Schema(description = "Major direction ID")
    private Long majorDirectionId;

    @Schema(description = "Status: PREPARING, ACTIVE")
    private String status;

    // Getters
    public String getClassName() { return className; }
    public String getShortName() { return shortName; }
    public Integer getStandardSize() { return standardSize; }
    public Integer getSortOrder() { return sortOrder; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getGradeId() { return gradeId; }
    public Long getMajorDirectionId() { return majorDirectionId; }
    public String getStatus() { return status; }

    // Setters
    public void setClassName(String className) { this.className = className; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public void setStandardSize(Integer standardSize) { this.standardSize = standardSize; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }
    public void setMajorDirectionId(Long majorDirectionId) { this.majorDirectionId = majorDirectionId; }
    public void setStatus(String status) { this.status = status; }
}
