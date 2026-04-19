package com.school.management.domain.student.model;

import com.school.management.domain.student.event.CohortCreatedEvent;
import com.school.management.domain.student.event.CohortStatusChangedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 入学年级(Cohort)聚合根
 * 代表一个入学年份的学生群体，是全校共享资源
 *
 * 年级与部门是正交关系：
 * - 一个年级可以包含多个部门的班级
 * - 一个部门可以有多个年级的班级
 */
public class Cohort extends AggregateRoot<Long> {

    private Long id;

    /**
     * 年级编码 (如: GRADE_2024)
     */
    private String gradeCode;

    /**
     * 年级名称 (如: 2024级)
     */
    private String gradeName;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 预计毕业年份
     */
    private Integer graduationYear;

    /**
     * 学制（年）
     */
    private Integer schoolingYears;

    /**
     * 年级主任ID
     */
    private Long directorId;

    /**
     * 年级主任姓名
     */
    private String directorName;

    /**
     * 年级辅导员ID
     */
    private Long counselorId;

    /**
     * 年级辅导员姓名
     */
    private String counselorName;

    /**
     * 标准班级人数（用于加权计算）
     */
    private Integer standardClassSize;

    /**
     * 年级状态
     */
    private CohortStatus status;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 备注
     */
    private String remarks;

    // 审计字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // For JPA/MyBatis
    protected Cohort() {
    }

    private Cohort(Builder builder) {
        this.id = builder.id;
        this.gradeCode = Objects.requireNonNull(builder.gradeCode, "gradeCode cannot be null");
        this.gradeName = Objects.requireNonNull(builder.gradeName, "gradeName cannot be null");
        this.enrollmentYear = Objects.requireNonNull(builder.enrollmentYear, "enrollmentYear cannot be null");
        this.schoolingYears = builder.schoolingYears != null ? builder.schoolingYears : 3;
        this.graduationYear = builder.graduationYear != null ? builder.graduationYear :
            this.enrollmentYear + this.schoolingYears;
        this.directorId = builder.directorId;
        this.directorName = builder.directorName;
        this.counselorId = builder.counselorId;
        this.counselorName = builder.counselorName;
        this.standardClassSize = builder.standardClassSize != null ? builder.standardClassSize : 50;
        this.status = builder.status != null ? builder.status : CohortStatus.ENROLLING;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.remarks = builder.remarks;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = builder.createdBy;

        validate();
    }

    /**
     * 工厂方法：创建新年级
     */
    public static Cohort create(String gradeCode, String gradeName, Integer enrollmentYear,
                               Integer schoolingYears, Long createdBy) {
        Cohort cohort = builder()
            .gradeCode(gradeCode)
            .gradeName(gradeName)
            .enrollmentYear(enrollmentYear)
            .schoolingYears(schoolingYears)
            .status(CohortStatus.ENROLLING)
            .createdBy(createdBy)
            .build();

        cohort.registerEvent(new CohortCreatedEvent(cohort));
        return cohort;
    }

    /**
     * 更新年级基本信息
     */
    public void updateInfo(String gradeName, Integer standardClassSize,
                           Integer sortOrder, String remarks, Long updatedBy) {
        if (gradeName != null && !gradeName.isBlank()) {
            this.gradeName = gradeName;
        }
        if (standardClassSize != null && standardClassSize > 0) {
            this.standardClassSize = standardClassSize;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.remarks = remarks;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 分配年级主任
     */
    public void assignDirector(Long directorId, String directorName, Long updatedBy) {
        this.directorId = directorId;
        this.directorName = directorName;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 分配年级辅导员
     */
    public void assignCounselor(Long counselorId, String counselorName, Long updatedBy) {
        this.counselorId = counselorId;
        this.counselorName = counselorName;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 激活年级（开始正常教学）
     */
    public void activate(Long updatedBy) {
        if (this.status != CohortStatus.ENROLLING) {
            throw new IllegalStateException("只有招生中的年级可以激活");
        }
        CohortStatus oldStatus = this.status;
        this.status = CohortStatus.ACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new CohortStatusChangedEvent(this, oldStatus, CohortStatus.ACTIVE));
    }

    /**
     * 年级毕业
     */
    public void graduate(Long updatedBy) {
        if (this.status != CohortStatus.ACTIVE) {
            throw new IllegalStateException("只有在读年级可以毕业");
        }
        CohortStatus oldStatus = this.status;
        this.status = CohortStatus.GRADUATED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new CohortStatusChangedEvent(this, oldStatus, CohortStatus.GRADUATED));
    }

    /**
     * 停止招生
     */
    public void stopEnrollment(Long updatedBy) {
        if (this.status == CohortStatus.GRADUATED) {
            throw new IllegalStateException("已毕业年级不能停止招生");
        }
        CohortStatus oldStatus = this.status;
        this.status = CohortStatus.SUSPENDED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new CohortStatusChangedEvent(this, oldStatus, CohortStatus.SUSPENDED));
    }

    /**
     * 检查年级是否可以添加班级
     */
    public boolean canAddClass() {
        return this.status == CohortStatus.ENROLLING || this.status == CohortStatus.ACTIVE;
    }

    /**
     * 检查年级是否在读
     */
    public boolean isActive() {
        return this.status == CohortStatus.ACTIVE;
    }

    /**
     * 检查年级是否已毕业
     */
    public boolean isGraduated() {
        return this.status == CohortStatus.GRADUATED;
    }

    private void validate() {
        if (gradeCode == null || gradeCode.isBlank()) {
            throw new IllegalArgumentException("年级编码不能为空");
        }
        if (gradeCode.length() > 50) {
            throw new IllegalArgumentException("年级编码不能超过50个字符");
        }
        if (gradeName == null || gradeName.isBlank()) {
            throw new IllegalArgumentException("年级名称不能为空");
        }
        if (gradeName.length() > 100) {
            throw new IllegalArgumentException("年级名称不能超过100个字符");
        }
        if (enrollmentYear < 2000 || enrollmentYear > 2100) {
            throw new IllegalArgumentException("入学年份无效");
        }
        if (schoolingYears < 1 || schoolingYears > 10) {
            throw new IllegalArgumentException("学制年限无效");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public Integer getSchoolingYears() {
        return schoolingYears;
    }

    public Long getDirectorId() {
        return directorId;
    }

    public String getDirectorName() {
        return directorName;
    }

    public Long getCounselorId() {
        return counselorId;
    }

    public String getCounselorName() {
        return counselorName;
    }

    public Integer getStandardClassSize() {
        return standardClassSize;
    }

    public CohortStatus getStatus() {
        return status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getRemarks() {
        return remarks;
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

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String gradeCode;
        private String gradeName;
        private Integer enrollmentYear;
        private Integer graduationYear;
        private Integer schoolingYears;
        private Long directorId;
        private String directorName;
        private Long counselorId;
        private String counselorName;
        private Integer standardClassSize;
        private CohortStatus status;
        private Integer sortOrder;
        private String remarks;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder gradeCode(String gradeCode) {
            this.gradeCode = gradeCode;
            return this;
        }

        public Builder gradeName(String gradeName) {
            this.gradeName = gradeName;
            return this;
        }

        public Builder enrollmentYear(Integer enrollmentYear) {
            this.enrollmentYear = enrollmentYear;
            return this;
        }

        public Builder graduationYear(Integer graduationYear) {
            this.graduationYear = graduationYear;
            return this;
        }

        public Builder schoolingYears(Integer schoolingYears) {
            this.schoolingYears = schoolingYears;
            return this;
        }

        public Builder directorId(Long directorId) {
            this.directorId = directorId;
            return this;
        }

        public Builder directorName(String directorName) {
            this.directorName = directorName;
            return this;
        }

        public Builder counselorId(Long counselorId) {
            this.counselorId = counselorId;
            return this;
        }

        public Builder counselorName(String counselorName) {
            this.counselorName = counselorName;
            return this;
        }

        public Builder standardClassSize(Integer standardClassSize) {
            this.standardClassSize = standardClassSize;
            return this;
        }

        public Builder status(CohortStatus status) {
            this.status = status;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Cohort build() {
            return new Cohort(this);
        }
    }
}
