package com.school.management.domain.student.model;

import com.school.management.domain.student.event.ClassCreatedEvent;
import com.school.management.domain.student.event.ClassStatusChangedEvent;
import com.school.management.domain.student.event.TeacherAssignedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 班级聚合根
 * 管理班级的核心业务逻辑，包括教师任职、状态变更等
 *
 * 注意：使用 SchoolClass 而非 Class 以避免与 Java 关键字冲突
 *
 * 核心关联设计：
 * - gradeId: 年级ID（必需），从Grade获取入学年份等信息
 * - majorDirectionId: 专业方向ID（必需），从MajorDirection可追溯到Major和OrgUnit
 * - orgUnitId: 保留此字段用于查询优化，但应与majorDirectionId派生的部门一致
 *
 * 移除的冗余字段（应从关联实体获取）：
 * - enrollmentYear: 从Grade获取
 * - majorId: 从MajorDirection获取
 * - orgUnitId: 从Major获取
 */
public class SchoolClass extends AggregateRoot<Long> {

    private Long id;

    /**
     * 班级编码（唯一标识）
     */
    private String classCode;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 班级简称
     */
    private String shortName;

    /**
     * 所属组织单元ID（系部）
     */
    private Long orgUnitId;

    /**
     * 所属年级ID
     */
    private Long gradeId;

    /**
     * 入学年份
     */
    private Integer enrollmentYear;

    /**
     * 年级级别（1-6，对应大一到大六或高一到高三等）
     */
    private Integer gradeLevel;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 学制（年）
     */
    private Integer schoolingYears;

    /**
     * 标准班级人数
     */
    private Integer standardSize;

    /**
     * 当前实际人数
     */
    private Integer currentSize;

    /**
     * 班级状态
     */
    private ClassStatus status;

    /**
     * 教师任职记录列表
     */
    private List<TeacherAssignment> teacherAssignments;

    /**
     * 排序号
     */
    private Integer sortOrder;

    // 审计字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;

    // For JPA/MyBatis
    protected SchoolClass() {
        this.teacherAssignments = new ArrayList<>();
    }

    private SchoolClass(Builder builder) {
        this.id = builder.id;
        this.classCode = Objects.requireNonNull(builder.classCode, "classCode cannot be null");
        this.className = Objects.requireNonNull(builder.className, "className cannot be null");
        this.shortName = builder.shortName;
        this.orgUnitId = Objects.requireNonNull(builder.orgUnitId, "orgUnitId cannot be null");
        this.gradeId = builder.gradeId;
        this.enrollmentYear = Objects.requireNonNull(builder.enrollmentYear, "enrollmentYear cannot be null");
        this.gradeLevel = builder.gradeLevel != null ? builder.gradeLevel : 1;
        this.majorDirectionId = builder.majorDirectionId;
        this.schoolingYears = builder.schoolingYears != null ? builder.schoolingYears : 3;
        this.standardSize = builder.standardSize != null ? builder.standardSize : 50;
        this.currentSize = builder.currentSize != null ? builder.currentSize : 0;
        this.status = builder.status != null ? builder.status : ClassStatus.PREPARING;
        this.teacherAssignments = builder.teacherAssignments != null
            ? new ArrayList<>(builder.teacherAssignments)
            : new ArrayList<>();
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = builder.createdBy;

        validate();
    }

    /**
     * 工厂方法：创建新班级
     */
    public static SchoolClass create(String classCode, String className, Long orgUnitId,
                                     Integer enrollmentYear, Long majorDirectionId,
                                     Long createdBy) {
        SchoolClass schoolClass = builder()
            .classCode(classCode)
            .className(className)
            .orgUnitId(orgUnitId)
            .enrollmentYear(enrollmentYear)
            .majorDirectionId(majorDirectionId)
            .status(ClassStatus.PREPARING)
            .createdBy(createdBy)
            .build();

        schoolClass.registerEvent(new ClassCreatedEvent(schoolClass));
        return schoolClass;
    }

    /**
     * 更新班级基本信息
     */
    public void updateInfo(String className, String shortName, Integer standardSize,
                           Integer sortOrder, Long updatedBy) {
        if (className != null && !className.isBlank()) {
            this.className = className;
        }
        this.shortName = shortName;
        if (standardSize != null && standardSize > 0) {
            this.standardSize = standardSize;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新班级组织关联信息
     */
    public void updateOrganization(Long orgUnitId, Long gradeId, Long majorDirectionId, Long updatedBy) {
        if (orgUnitId != null) {
            this.orgUnitId = orgUnitId;
        }
        if (gradeId != null) {
            this.gradeId = gradeId;
        }
        if (majorDirectionId != null) {
            this.majorDirectionId = majorDirectionId;
        }
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 激活班级（开始招生/正式开班）
     */
    public void activate(Long updatedBy) {
        if (this.status != ClassStatus.PREPARING) {
            throw new IllegalStateException("Only preparing class can be activated");
        }
        ClassStatus oldStatus = this.status;
        this.status = ClassStatus.ACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new ClassStatusChangedEvent(this, oldStatus, ClassStatus.ACTIVE));
    }

    /**
     * 班级毕业
     */
    public void graduate(Long updatedBy) {
        if (this.status != ClassStatus.ACTIVE) {
            throw new IllegalStateException("Only active class can graduate");
        }
        ClassStatus oldStatus = this.status;
        this.status = ClassStatus.GRADUATED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        // 结束所有当前任职
        endAllCurrentAssignments();

        registerEvent(new ClassStatusChangedEvent(this, oldStatus, ClassStatus.GRADUATED));
    }

    /**
     * 撤销班级
     */
    public void dissolve(Long updatedBy) {
        if (this.status == ClassStatus.GRADUATED || this.status == ClassStatus.DISSOLVED) {
            throw new IllegalStateException("Cannot dissolve graduated or already dissolved class");
        }
        ClassStatus oldStatus = this.status;
        this.status = ClassStatus.DISSOLVED;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        // 结束所有当前任职
        endAllCurrentAssignments();

        registerEvent(new ClassStatusChangedEvent(this, oldStatus, ClassStatus.DISSOLVED));
    }

    /**
     * 分配班主任
     */
    public void assignHeadTeacher(Long teacherId, String teacherName, Long updatedBy) {
        // 先结束当前班主任的任职
        getCurrentHeadTeacher().ifPresent(current ->
            endTeacherAssignment(current.getTeacherId(), TeacherAssignment.TeacherRole.HEAD_TEACHER)
        );

        TeacherAssignment assignment = TeacherAssignment.create(
            teacherId, teacherName,
            TeacherAssignment.TeacherRole.HEAD_TEACHER,
            LocalDate.now()
        );
        this.teacherAssignments.add(assignment);
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new TeacherAssignedEvent(this, assignment));
    }

    /**
     * 分配副班主任
     */
    public void assignDeputyHeadTeacher(Long teacherId, String teacherName, Long updatedBy) {
        TeacherAssignment assignment = TeacherAssignment.create(
            teacherId, teacherName,
            TeacherAssignment.TeacherRole.DEPUTY_HEAD_TEACHER,
            LocalDate.now()
        );
        this.teacherAssignments.add(assignment);
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new TeacherAssignedEvent(this, assignment));
    }

    /**
     * 结束教师任职
     */
    public void endTeacherAssignment(Long teacherId, TeacherAssignment.TeacherRole role) {
        this.teacherAssignments = this.teacherAssignments.stream()
            .map(a -> {
                if (a.getTeacherId().equals(teacherId) && a.getRole() == role && a.isCurrent()) {
                    return a.endAssignment(LocalDate.now());
                }
                return a;
            })
            .collect(Collectors.toList());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 结束所有当前任职
     */
    private void endAllCurrentAssignments() {
        this.teacherAssignments = this.teacherAssignments.stream()
            .map(a -> a.isCurrent() ? a.endAssignment(LocalDate.now()) : a)
            .collect(Collectors.toList());
    }

    /**
     * 获取当前班主任
     */
    public Optional<TeacherAssignment> getCurrentHeadTeacher() {
        return teacherAssignments.stream()
            .filter(a -> a.isCurrent() && a.getRole() == TeacherAssignment.TeacherRole.HEAD_TEACHER)
            .findFirst();
    }

    /**
     * 获取当前副班主任列表
     */
    public List<TeacherAssignment> getCurrentDeputyHeadTeachers() {
        return teacherAssignments.stream()
            .filter(a -> a.isCurrent() && a.getRole() == TeacherAssignment.TeacherRole.DEPUTY_HEAD_TEACHER)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有当前任职教师
     */
    public List<TeacherAssignment> getCurrentTeachers() {
        return teacherAssignments.stream()
            .filter(TeacherAssignment::isCurrent)
            .collect(Collectors.toList());
    }

    /**
     * 更新学生人数
     */
    public void updateCurrentSize(int newSize) {
        if (newSize < 0) {
            throw new IllegalArgumentException("Current size cannot be negative");
        }
        this.currentSize = newSize;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 增加学生
     */
    public void incrementSize() {
        this.currentSize++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 减少学生
     */
    public void decrementSize() {
        if (this.currentSize > 0) {
            this.currentSize--;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 计算预计毕业年份
     */
    public int getExpectedGraduationYear() {
        return enrollmentYear + schoolingYears;
    }

    /**
     * 检查是否已满员
     */
    public boolean isFull() {
        return currentSize >= standardSize;
    }

    /**
     * 计算空余名额
     */
    public int getAvailableSlots() {
        return Math.max(0, standardSize - currentSize);
    }

    private void validate() {
        if (classCode == null || classCode.isBlank()) {
            throw new IllegalArgumentException("Class code cannot be empty");
        }
        if (classCode.length() > 50) {
            throw new IllegalArgumentException("Class code cannot exceed 50 characters");
        }
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Class name cannot be empty");
        }
        if (enrollmentYear < 2000 || enrollmentYear > 2100) {
            throw new IllegalArgumentException("Invalid enrollment year");
        }
        if (gradeLevel < 1 || gradeLevel > 10) {
            throw new IllegalArgumentException("Invalid grade level");
        }
        if (schoolingYears < 1 || schoolingYears > 10) {
            throw new IllegalArgumentException("Invalid schooling years");
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

    public String getClassCode() {
        return classCode;
    }

    public String getClassName() {
        return className;
    }

    public String getShortName() {
        return shortName;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public Integer getEnrollmentYear() {
        return enrollmentYear;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public Long getMajorDirectionId() {
        return majorDirectionId;
    }

    public Integer getSchoolingYears() {
        return schoolingYears;
    }

    public Integer getStandardSize() {
        return standardSize;
    }

    public Integer getCurrentSize() {
        return currentSize;
    }

    public ClassStatus getStatus() {
        return status;
    }

    public List<TeacherAssignment> getTeacherAssignments() {
        return Collections.unmodifiableList(teacherAssignments);
    }

    public Integer getSortOrder() {
        return sortOrder;
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
        private String classCode;
        private String className;
        private String shortName;
        private Long orgUnitId;
        private Long gradeId;
        private Integer enrollmentYear;
        private Integer gradeLevel;
        private Long majorDirectionId;
        private Integer schoolingYears;
        private Integer standardSize;
        private Integer currentSize;
        private ClassStatus status;
        private List<TeacherAssignment> teacherAssignments;
        private Integer sortOrder;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder classCode(String classCode) {
            this.classCode = classCode;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder shortName(String shortName) {
            this.shortName = shortName;
            return this;
        }

        public Builder orgUnitId(Long orgUnitId) {
            this.orgUnitId = orgUnitId;
            return this;
        }

        public Builder gradeId(Long gradeId) {
            this.gradeId = gradeId;
            return this;
        }

        public Builder enrollmentYear(Integer enrollmentYear) {
            this.enrollmentYear = enrollmentYear;
            return this;
        }

        public Builder gradeLevel(Integer gradeLevel) {
            this.gradeLevel = gradeLevel;
            return this;
        }

        public Builder majorDirectionId(Long majorDirectionId) {
            this.majorDirectionId = majorDirectionId;
            return this;
        }

        public Builder schoolingYears(Integer schoolingYears) {
            this.schoolingYears = schoolingYears;
            return this;
        }

        public Builder standardSize(Integer standardSize) {
            this.standardSize = standardSize;
            return this;
        }

        public Builder currentSize(Integer currentSize) {
            this.currentSize = currentSize;
            return this;
        }

        public Builder status(ClassStatus status) {
            this.status = status;
            return this;
        }

        public Builder teacherAssignments(List<TeacherAssignment> teacherAssignments) {
            this.teacherAssignments = teacherAssignments;
            return this;
        }

        public Builder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public SchoolClass build() {
            return new SchoolClass(this);
        }
    }
}
