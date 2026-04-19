package com.school.management.domain.student.model;

import com.school.management.domain.shared.ValueObject;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 教师任职记录值对象
 * 记录教师在班级的任职信息，包括班主任、副班主任等角色
 */
public class TeacherAssignment implements ValueObject {

    private static final long serialVersionUID = 1L;

    /**
     * 教师ID
     */
    private final Long teacherId;

    /**
     * 教师姓名（冗余存储，便于展示）
     */
    private final String teacherName;

    /**
     * 任职角色
     */
    private final TeacherRole role;

    /**
     * 任职开始日期
     */
    private final LocalDate startDate;

    /**
     * 任职结束日期（null表示当前任职中）
     */
    private final LocalDate endDate;

    /**
     * 是否为当前任职
     */
    private final boolean current;

    /**
     * 教师角色枚举
     */
    public enum TeacherRole {
        HEAD_TEACHER("班主任"),
        DEPUTY_HEAD_TEACHER("副班主任"),
        SUBJECT_TEACHER("任课教师"),
        COUNSELOR("辅导员");

        private final String description;

        TeacherRole(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private TeacherAssignment(Long teacherId, String teacherName, TeacherRole role,
                              LocalDate startDate, LocalDate endDate, boolean current) {
        this.teacherId = Objects.requireNonNull(teacherId, "teacherId cannot be null");
        this.teacherName = Objects.requireNonNull(teacherName, "teacherName cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
        this.endDate = endDate;
        this.current = current;
        validate();
    }

    /**
     * 创建新的教师任职记录
     */
    public static TeacherAssignment create(Long teacherId, String teacherName,
                                           TeacherRole role, LocalDate startDate) {
        return new TeacherAssignment(teacherId, teacherName, role, startDate, null, true);
    }

    /**
     * 创建已结束的任职记录
     */
    public static TeacherAssignment createEnded(Long teacherId, String teacherName,
                                                 TeacherRole role, LocalDate startDate,
                                                 LocalDate endDate) {
        return new TeacherAssignment(teacherId, teacherName, role, startDate, endDate, false);
    }

    /**
     * 结束当前任职
     */
    public TeacherAssignment endAssignment(LocalDate endDate) {
        if (!this.current) {
            throw new IllegalStateException("Assignment already ended");
        }
        return new TeacherAssignment(this.teacherId, this.teacherName, this.role,
                                     this.startDate, endDate, false);
    }

    @Override
    public void validate() {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    // Getters
    public Long getTeacherId() {
        return teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public TeacherRole getRole() {
        return role;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isCurrent() {
        return current;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeacherAssignment that = (TeacherAssignment) o;
        return Objects.equals(teacherId, that.teacherId) &&
               role == that.role &&
               Objects.equals(startDate, that.startDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId, role, startDate);
    }

    @Override
    public String toString() {
        return String.format("TeacherAssignment{teacherId=%d, name='%s', role=%s, current=%s}",
                             teacherId, teacherName, role, current);
    }
}
