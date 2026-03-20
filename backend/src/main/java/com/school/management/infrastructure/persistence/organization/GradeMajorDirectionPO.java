package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 年级-专业方向关联 持久化对象
 * 映射到 grade_major_directions 表
 */
@TableName("grade_major_directions")
public class GradeMajorDirectionPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long gradeId;
    private Long majorDirectionId;
    private Long majorId;
    private String majorName;

    private Integer plannedClassCount;
    private Integer plannedStudentCount;
    private Integer actualClassCount;
    private Integer actualStudentCount;

    private String remarks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;
    private Long updatedBy;

    @TableLogic
    private Integer deleted;

    // --- JOIN-only fields (not persisted) ---

    @TableField(exist = false)
    private String directionName;

    @TableField(exist = false)
    private String directionCode;

    @TableField(exist = false)
    private String level;

    @TableField(exist = false)
    private Integer years;

    @TableField(exist = false)
    private Integer isSegmented;

    @TableField(exist = false)
    private String phase1Level;

    @TableField(exist = false)
    private Integer phase1Years;

    @TableField(exist = false)
    private String phase2Level;

    @TableField(exist = false)
    private Integer phase2Years;

    @TableField(exist = false)
    private Long orgUnitId;

    @TableField(exist = false)
    private Integer enrollmentYear;

    @TableField(exist = false)
    private String majorCode;

    // Getters
    public Long getId() { return id; }
    public Long getGradeId() { return gradeId; }
    public Long getMajorDirectionId() { return majorDirectionId; }
    public Long getMajorId() { return majorId; }
    public String getMajorName() { return majorName; }
    public Integer getPlannedClassCount() { return plannedClassCount; }
    public Integer getPlannedStudentCount() { return plannedStudentCount; }
    public Integer getActualClassCount() { return actualClassCount; }
    public Integer getActualStudentCount() { return actualStudentCount; }
    public String getRemarks() { return remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public Integer getDeleted() { return deleted; }
    public String getDirectionName() { return directionName; }
    public String getDirectionCode() { return directionCode; }
    public String getLevel() { return level; }
    public Integer getYears() { return years; }
    public Integer getIsSegmented() { return isSegmented; }
    public String getPhase1Level() { return phase1Level; }
    public Integer getPhase1Years() { return phase1Years; }
    public String getPhase2Level() { return phase2Level; }
    public Integer getPhase2Years() { return phase2Years; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Integer getEnrollmentYear() { return enrollmentYear; }
    public String getMajorCode() { return majorCode; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }
    public void setMajorDirectionId(Long majorDirectionId) { this.majorDirectionId = majorDirectionId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }
    public void setMajorName(String majorName) { this.majorName = majorName; }
    public void setPlannedClassCount(Integer plannedClassCount) { this.plannedClassCount = plannedClassCount; }
    public void setPlannedStudentCount(Integer plannedStudentCount) { this.plannedStudentCount = plannedStudentCount; }
    public void setActualClassCount(Integer actualClassCount) { this.actualClassCount = actualClassCount; }
    public void setActualStudentCount(Integer actualStudentCount) { this.actualStudentCount = actualStudentCount; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setDirectionName(String directionName) { this.directionName = directionName; }
    public void setDirectionCode(String directionCode) { this.directionCode = directionCode; }
    public void setLevel(String level) { this.level = level; }
    public void setYears(Integer years) { this.years = years; }
    public void setIsSegmented(Integer isSegmented) { this.isSegmented = isSegmented; }
    public void setPhase1Level(String phase1Level) { this.phase1Level = phase1Level; }
    public void setPhase1Years(Integer phase1Years) { this.phase1Years = phase1Years; }
    public void setPhase2Level(String phase2Level) { this.phase2Level = phase2Level; }
    public void setPhase2Years(Integer phase2Years) { this.phase2Years = phase2Years; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
    public void setEnrollmentYear(Integer enrollmentYear) { this.enrollmentYear = enrollmentYear; }
    public void setMajorCode(String majorCode) { this.majorCode = majorCode; }
}
