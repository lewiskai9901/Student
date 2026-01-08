package com.school.management.domain.organization.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 年级开设专业方向
 * 记录每个年级开设的专业方向，用于约束班级创建
 *
 * 业务规则：
 * - 班级的 majorDirectionId 必须在该年级开设的专业方向列表中
 * - 同一年级不能重复开设同一专业方向
 */
public class GradeOpenedDirection extends Entity<Long> {

    private Long id;

    /**
     * 年级ID（外键关联 Grade）
     */
    private Long gradeId;

    /**
     * 专业方向ID（外键关联 MajorDirection）
     */
    private Long majorDirectionId;

    /**
     * 计划班级数
     */
    private Integer plannedClasses;

    /**
     * 计划招生人数
     */
    private Integer plannedStudents;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 是否启用
     */
    private Boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // For JPA/MyBatis
    protected GradeOpenedDirection() {
    }

    private GradeOpenedDirection(Long gradeId, Long majorDirectionId,
                                  Integer plannedClasses, Integer plannedStudents,
                                  String remarks, Long createdBy) {
        this.gradeId = Objects.requireNonNull(gradeId, "gradeId cannot be null");
        this.majorDirectionId = Objects.requireNonNull(majorDirectionId, "majorDirectionId cannot be null");
        this.plannedClasses = plannedClasses;
        this.plannedStudents = plannedStudents;
        this.remarks = remarks;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = createdBy;
    }

    /**
     * 工厂方法
     */
    public static GradeOpenedDirection create(Long gradeId, Long majorDirectionId,
                                               Integer plannedClasses, Integer plannedStudents,
                                               String remarks, Long createdBy) {
        return new GradeOpenedDirection(gradeId, majorDirectionId,
            plannedClasses, plannedStudents, remarks, createdBy);
    }

    /**
     * 用于从数据库重建
     */
    public static GradeOpenedDirection reconstitute(Long id, Long gradeId, Long majorDirectionId,
                                                     Integer plannedClasses, Integer plannedStudents,
                                                     String remarks, Boolean enabled,
                                                     LocalDateTime createdAt, LocalDateTime updatedAt,
                                                     Long createdBy, Long updatedBy) {
        GradeOpenedDirection entity = new GradeOpenedDirection();
        entity.id = id;
        entity.gradeId = gradeId;
        entity.majorDirectionId = majorDirectionId;
        entity.plannedClasses = plannedClasses;
        entity.plannedStudents = plannedStudents;
        entity.remarks = remarks;
        entity.enabled = enabled != null ? enabled : true;
        entity.createdAt = createdAt;
        entity.updatedAt = updatedAt;
        entity.createdBy = createdBy;
        entity.updatedBy = updatedBy;
        return entity;
    }

    /**
     * 更新招生计划
     */
    public void updatePlan(Integer plannedClasses, Integer plannedStudents,
                           String remarks, Long updatedBy) {
        this.plannedClasses = plannedClasses;
        this.plannedStudents = plannedStudents;
        this.remarks = remarks;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用
     */
    public void enable(Long updatedBy) {
        this.enabled = true;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用
     */
    public void disable(Long updatedBy) {
        this.enabled = false;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public Long getMajorDirectionId() {
        return majorDirectionId;
    }

    public Integer getPlannedClasses() {
        return plannedClasses;
    }

    public Integer getPlannedStudents() {
        return plannedStudents;
    }

    public String getRemarks() {
        return remarks;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
