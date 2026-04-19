package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Major Direction Persistence Object
 */
@TableName(value = "major_directions", autoResultMap = true)
public class MajorDirectionPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long majorId;
    private String directionName;
    private String directionCode;

    @TableField("level")
    private String level;

    private Integer years;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private String remarks;

    // ========== 新增字段: 技工院校增强 ==========

    private String enrollmentTarget;
    private String educationForm;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> certificateNames;

    private String trainingStandard;
    private String cooperationEnterprise;
    private Integer maxEnrollment;
    private Integer sortOrder;
    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long createdBy;
    private Long updatedBy;

    @TableLogic
    private Integer deleted;

    // Getters
    public Long getId() { return id; }
    public Long getMajorId() { return majorId; }
    public String getDirectionName() { return directionName; }
    public String getDirectionCode() { return directionCode; }
    public String getLevel() { return level; }
    public Integer getYears() { return years; }
    public Integer getIsSegmented() { return isSegmented; }
    public String getPhase1Level() { return phase1Level; }
    public Integer getPhase1Years() { return phase1Years; }
    public String getPhase2Level() { return phase2Level; }
    public Integer getPhase2Years() { return phase2Years; }
    public String getRemarks() { return remarks; }
    public String getEnrollmentTarget() { return enrollmentTarget; }
    public String getEducationForm() { return educationForm; }
    public List<String> getCertificateNames() { return certificateNames; }
    public String getTrainingStandard() { return trainingStandard; }
    public String getCooperationEnterprise() { return cooperationEnterprise; }
    public Integer getMaxEnrollment() { return maxEnrollment; }
    public Integer getSortOrder() { return sortOrder; }
    public Integer getEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public Integer getDeleted() { return deleted; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }
    public void setDirectionName(String directionName) { this.directionName = directionName; }
    public void setDirectionCode(String directionCode) { this.directionCode = directionCode; }
    public void setLevel(String level) { this.level = level; }
    public void setYears(Integer years) { this.years = years; }
    public void setIsSegmented(Integer isSegmented) { this.isSegmented = isSegmented; }
    public void setPhase1Level(String phase1Level) { this.phase1Level = phase1Level; }
    public void setPhase1Years(Integer phase1Years) { this.phase1Years = phase1Years; }
    public void setPhase2Level(String phase2Level) { this.phase2Level = phase2Level; }
    public void setPhase2Years(Integer phase2Years) { this.phase2Years = phase2Years; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setEnrollmentTarget(String enrollmentTarget) { this.enrollmentTarget = enrollmentTarget; }
    public void setEducationForm(String educationForm) { this.educationForm = educationForm; }
    public void setCertificateNames(List<String> certificateNames) { this.certificateNames = certificateNames; }
    public void setTrainingStandard(String trainingStandard) { this.trainingStandard = trainingStandard; }
    public void setCooperationEnterprise(String cooperationEnterprise) { this.cooperationEnterprise = cooperationEnterprise; }
    public void setMaxEnrollment(Integer maxEnrollment) { this.maxEnrollment = maxEnrollment; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
