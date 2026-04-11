package com.school.management.domain.academic.model;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 课程聚合根
 */
public class Course extends AggregateRoot<Long> {

    private Long id;

    /** 课程代码 */
    private String courseCode;

    /** 课程名称 */
    private String courseName;

    /** 课程英文名称 */
    private String courseNameEn;

    /** 课程类别: 1-公共基础课 2-专业核心课 3-专业方向课 4-选修课 */
    private Integer courseCategory;

    /** 课程类型: 1-理论 2-实践 3-理论+实践 */
    private Integer courseType;

    /** 课程性质: 1-必修 2-限选 3-任选 */
    private Integer courseNature;

    /** 学分 */
    private BigDecimal credits;

    /** 总学时 */
    private Integer totalHours;

    /** 理论学时 */
    private Integer theoryHours;

    /** 实践学时 */
    private Integer practiceHours;

    /** 周学时 */
    private Integer weeklyHours;

    /** 考核方式: 1-考试 2-考查 3-技能考试 4-考试+考查 */
    private Integer assessmentMethod;

    /** 开课组织单元ID */
    private Long orgUnitId;

    /** 课程描述 */
    private String description;

    /** 状态: 1-启用 0-停用 */
    private Integer status;

    // 审计字段
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Course() {}

    private Course(Builder builder) {
        this.id = builder.id;
        this.courseCode = Objects.requireNonNull(builder.courseCode, "courseCode cannot be null");
        this.courseName = Objects.requireNonNull(builder.courseName, "courseName cannot be null");
        this.courseNameEn = builder.courseNameEn;
        this.courseCategory = builder.courseCategory != null ? builder.courseCategory : 1;
        this.courseType = builder.courseType != null ? builder.courseType : 1;
        this.courseNature = builder.courseNature != null ? builder.courseNature : 1;
        this.credits = builder.credits != null ? builder.credits : BigDecimal.ZERO;
        this.totalHours = builder.totalHours != null ? builder.totalHours : 0;
        this.theoryHours = builder.theoryHours != null ? builder.theoryHours : 0;
        this.practiceHours = builder.practiceHours != null ? builder.practiceHours : 0;
        this.weeklyHours = builder.weeklyHours != null ? builder.weeklyHours : 2;
        this.assessmentMethod = builder.assessmentMethod != null ? builder.assessmentMethod : 1;
        this.orgUnitId = builder.orgUnitId;
        this.description = builder.description;
        this.status = builder.status != null ? builder.status : 1;
        this.createdBy = builder.createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        validate();
    }

    /**
     * 工厂方法：创建课程
     */
    public static Course create(String courseCode, String courseName, Long createdBy) {
        return builder()
            .courseCode(courseCode)
            .courseName(courseName)
            .createdBy(createdBy)
            .build();
    }

    /**
     * 更新课程信息
     */
    public void update(String courseName, String courseNameEn,
                       Integer courseCategory, Integer courseType, Integer courseNature,
                       BigDecimal credits, Integer totalHours, Integer theoryHours,
                       Integer practiceHours, Integer weeklyHours, Integer assessmentMethod,
                       Long orgUnitId, String description, Long updatedBy) {
        if (courseName != null) this.courseName = courseName;
        if (courseNameEn != null) this.courseNameEn = courseNameEn;
        if (courseCategory != null) this.courseCategory = courseCategory;
        if (courseType != null) this.courseType = courseType;
        if (courseNature != null) this.courseNature = courseNature;
        if (credits != null) this.credits = credits;
        if (totalHours != null) this.totalHours = totalHours;
        if (theoryHours != null) this.theoryHours = theoryHours;
        if (practiceHours != null) this.practiceHours = practiceHours;
        if (weeklyHours != null) this.weeklyHours = weeklyHours;
        if (assessmentMethod != null) this.assessmentMethod = assessmentMethod;
        this.orgUnitId = orgUnitId; // allow null to clear
        if (description != null) this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新状态
     */
    public void updateStatus(Integer status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.updatedAt = LocalDateTime.now();
    }

    private void validate() {
        if (courseCode == null || courseCode.isBlank()) {
            throw new IllegalArgumentException("Course code cannot be empty");
        }
        if (courseCode.length() > 50) {
            throw new IllegalArgumentException("Course code cannot exceed 50 characters");
        }
        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException("Course name cannot be empty");
        }
        if (courseName.length() > 100) {
            throw new IllegalArgumentException("Course name cannot exceed 100 characters");
        }
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getCourseNameEn() { return courseNameEn; }
    public Integer getCourseCategory() { return courseCategory; }
    public Integer getCourseType() { return courseType; }
    public Integer getCourseNature() { return courseNature; }
    public BigDecimal getCredits() { return credits; }
    public Integer getTotalHours() { return totalHours; }
    public Integer getTheoryHours() { return theoryHours; }
    public Integer getPracticeHours() { return practiceHours; }
    public Integer getWeeklyHours() { return weeklyHours; }
    public Integer getAssessmentMethod() { return assessmentMethod; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getDescription() { return description; }
    public Integer getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private String courseCode;
        private String courseName;
        private String courseNameEn;
        private Integer courseCategory;
        private Integer courseType;
        private Integer courseNature;
        private BigDecimal credits;
        private Integer totalHours;
        private Integer theoryHours;
        private Integer practiceHours;
        private Integer weeklyHours;
        private Integer assessmentMethod;
        private Long orgUnitId;
        private String description;
        private Integer status;
        private Long createdBy;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder courseCode(String v) { this.courseCode = v; return this; }
        public Builder courseName(String v) { this.courseName = v; return this; }
        public Builder courseNameEn(String v) { this.courseNameEn = v; return this; }
        public Builder courseCategory(Integer v) { this.courseCategory = v; return this; }
        public Builder courseType(Integer v) { this.courseType = v; return this; }
        public Builder courseNature(Integer v) { this.courseNature = v; return this; }
        public Builder credits(BigDecimal v) { this.credits = v; return this; }
        public Builder totalHours(Integer v) { this.totalHours = v; return this; }
        public Builder theoryHours(Integer v) { this.theoryHours = v; return this; }
        public Builder practiceHours(Integer v) { this.practiceHours = v; return this; }
        public Builder weeklyHours(Integer v) { this.weeklyHours = v; return this; }
        public Builder assessmentMethod(Integer v) { this.assessmentMethod = v; return this; }
        public Builder orgUnitId(Long v) { this.orgUnitId = v; return this; }
        public Builder description(String v) { this.description = v; return this; }
        public Builder status(Integer v) { this.status = v; return this; }
        public Builder createdBy(Long v) { this.createdBy = v; return this; }

        public Course build() { return new Course(this); }
    }
}
