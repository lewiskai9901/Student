package com.school.management.domain.academic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 方案课程实体
 * 属于 CurriculumPlan 聚合，但独立持久化
 */
public class PlanCourse {

    private Long id;

    /** 所属培养方案ID */
    private Long planId;

    /** 课程ID */
    private Long courseId;

    /** 学期号 */
    private Integer semesterNumber;

    /** 课程类别(方案内可覆盖) */
    private Integer courseCategory;

    /** 课程类型(方案内可覆盖) */
    private Integer courseType;

    /** 学分(方案内可覆盖) */
    private BigDecimal credits;

    /** 总学时 */
    private Integer totalHours;

    /** 周学时 */
    private Integer weeklyHours;

    /** 理论学时 */
    private Integer theoryHours;

    /** 实践学时 */
    private Integer practiceHours;

    /** 考核方式 */
    private Integer assessmentMethod;

    /** 排序 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    // 非持久化字段，用于查询展示
    private String courseCode;
    private String courseName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected PlanCourse() {}

    private PlanCourse(Builder builder) {
        this.id = builder.id;
        this.planId = builder.planId;
        this.courseId = builder.courseId;
        this.semesterNumber = builder.semesterNumber;
        this.courseCategory = builder.courseCategory;
        this.courseType = builder.courseType;
        this.credits = builder.credits;
        this.totalHours = builder.totalHours;
        this.weeklyHours = builder.weeklyHours;
        this.theoryHours = builder.theoryHours;
        this.practiceHours = builder.practiceHours;
        this.assessmentMethod = builder.assessmentMethod;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.remark = builder.remark;
        this.courseCode = builder.courseCode;
        this.courseName = builder.courseName;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : this.createdAt;
    }

    /**
     * 更新方案课程信息
     */
    public void update(Integer semesterNumber, Integer courseCategory, Integer courseType,
                       BigDecimal credits, Integer totalHours, Integer weeklyHours,
                       Integer theoryHours, Integer practiceHours,
                       Integer assessmentMethod, Integer sortOrder, String remark) {
        if (semesterNumber != null) this.semesterNumber = semesterNumber;
        if (courseCategory != null) this.courseCategory = courseCategory;
        if (courseType != null) this.courseType = courseType;
        if (credits != null) this.credits = credits;
        if (totalHours != null) this.totalHours = totalHours;
        if (weeklyHours != null) this.weeklyHours = weeklyHours;
        if (theoryHours != null) this.theoryHours = theoryHours;
        if (practiceHours != null) this.practiceHours = practiceHours;
        if (assessmentMethod != null) this.assessmentMethod = assessmentMethod;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (remark != null) this.remark = remark;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 创建副本（用于 copyPlan），绑定到新方案
     */
    public PlanCourse copyForPlan(Long newPlanId) {
        return builder()
            .planId(newPlanId)
            .courseId(this.courseId)
            .semesterNumber(this.semesterNumber)
            .courseCategory(this.courseCategory)
            .courseType(this.courseType)
            .credits(this.credits)
            .totalHours(this.totalHours)
            .weeklyHours(this.weeklyHours)
            .theoryHours(this.theoryHours)
            .practiceHours(this.practiceHours)
            .assessmentMethod(this.assessmentMethod)
            .sortOrder(this.sortOrder)
            .remark(this.remark)
            .build();
    }

    // Getters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public Long getCourseId() { return courseId; }
    public Integer getSemesterNumber() { return semesterNumber; }
    public Integer getCourseCategory() { return courseCategory; }
    public Integer getCourseType() { return courseType; }
    public BigDecimal getCredits() { return credits; }
    public Integer getTotalHours() { return totalHours; }
    public Integer getWeeklyHours() { return weeklyHours; }
    public Integer getTheoryHours() { return theoryHours; }
    public Integer getPracticeHours() { return practiceHours; }
    public Integer getAssessmentMethod() { return assessmentMethod; }
    public Integer getSortOrder() { return sortOrder; }
    public String getRemark() { return remark; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long planId;
        private Long courseId;
        private Integer semesterNumber;
        private Integer courseCategory;
        private Integer courseType;
        private BigDecimal credits;
        private Integer totalHours;
        private Integer weeklyHours;
        private Integer theoryHours;
        private Integer practiceHours;
        private Integer assessmentMethod;
        private Integer sortOrder;
        private String remark;
        private String courseCode;
        private String courseName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder planId(Long v) { this.planId = v; return this; }
        public Builder courseId(Long v) { this.courseId = v; return this; }
        public Builder semesterNumber(Integer v) { this.semesterNumber = v; return this; }
        public Builder courseCategory(Integer v) { this.courseCategory = v; return this; }
        public Builder courseType(Integer v) { this.courseType = v; return this; }
        public Builder credits(BigDecimal v) { this.credits = v; return this; }
        public Builder totalHours(Integer v) { this.totalHours = v; return this; }
        public Builder weeklyHours(Integer v) { this.weeklyHours = v; return this; }
        public Builder theoryHours(Integer v) { this.theoryHours = v; return this; }
        public Builder practiceHours(Integer v) { this.practiceHours = v; return this; }
        public Builder assessmentMethod(Integer v) { this.assessmentMethod = v; return this; }
        public Builder sortOrder(Integer v) { this.sortOrder = v; return this; }
        public Builder remark(String v) { this.remark = v; return this; }
        public Builder courseCode(String v) { this.courseCode = v; return this; }
        public Builder courseName(String v) { this.courseName = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { this.updatedAt = v; return this; }

        public PlanCourse build() { return new PlanCourse(this); }
    }
}
