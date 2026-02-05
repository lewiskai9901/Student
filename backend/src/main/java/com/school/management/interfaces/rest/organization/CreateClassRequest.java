package com.school.management.interfaces.rest.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Create class request
 */
@Schema(description = "Create class request")
public class CreateClassRequest {

    @NotBlank(message = "Class code required")
    @Schema(description = "Class code", required = true, example = "2024-CS-01")
    private String classCode;

    @NotBlank(message = "Class name required")
    @Schema(description = "Class name", required = true, example = "2024 Computer Science Class 1")
    private String className;

    @Schema(description = "Short name", example = "CS-1")
    private String shortName;

    @NotNull(message = "OrgUnit ID required")
    @Schema(description = "Organization unit ID", required = true)
    private Long orgUnitId;

    @NotNull(message = "Enrollment year required")
    @Schema(description = "Enrollment year", required = true, example = "2024")
    private Integer enrollmentYear;

    @Schema(description = "Grade level", example = "1")
    private Integer gradeLevel;

    @Schema(description = "Major direction ID")
    private Long majorDirectionId;

    @Schema(description = "Schooling years", example = "4")
    private Integer schoolingYears;

    @Schema(description = "Standard class size", example = "50")
    private Integer standardSize;

    @Schema(description = "Class status (0=preparing, 1=active, 2=graduated, 3=cancelled)", example = "1")
    private Integer status;

    // Getters
    public String getClassCode() { return classCode; }
    public String getClassName() { return className; }
    public String getShortName() { return shortName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Integer getEnrollmentYear() { return enrollmentYear; }
    public Integer getGradeLevel() { return gradeLevel; }
    public Long getMajorDirectionId() { return majorDirectionId; }
    public Integer getSchoolingYears() { return schoolingYears; }
    public Integer getStandardSize() { return standardSize; }
    public Integer getStatus() { return status; }

    // Setters
    public void setClassCode(String classCode) { this.classCode = classCode; }
    public void setClassName(String className) { this.className = className; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public void setEnrollmentYear(Integer enrollmentYear) { this.enrollmentYear = enrollmentYear; }
    public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
    public void setMajorDirectionId(Long majorDirectionId) { this.majorDirectionId = majorDirectionId; }
    public void setSchoolingYears(Integer schoolingYears) { this.schoolingYears = schoolingYears; }
    public void setStandardSize(Integer standardSize) { this.standardSize = standardSize; }
    public void setStatus(Integer status) { this.status = status; }
}
