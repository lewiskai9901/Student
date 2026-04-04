package com.school.management.domain.student.event;

import com.school.management.domain.student.model.SchoolClass;
import com.school.management.domain.student.model.TeacherAssignment;
import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

/**
 * 教师任职分配事件
 */
public class TeacherAssignedEvent extends BaseDomainEvent {

    private final Long classId;
    private final String classCode;
    private final String className;
    private final Long teacherId;
    private final String teacherName;
    private final TeacherAssignment.TeacherRole role;
    private final LocalDate startDate;

    public TeacherAssignedEvent(SchoolClass schoolClass, TeacherAssignment assignment) {
        super("SchoolClass", String.valueOf(schoolClass.getId()));
        this.classId = schoolClass.getId();
        this.classCode = schoolClass.getClassCode();
        this.className = schoolClass.getClassName();
        this.teacherId = assignment.getTeacherId();
        this.teacherName = assignment.getTeacherName();
        this.role = assignment.getRole();
        this.startDate = assignment.getStartDate();
    }

    public Long getClassId() {
        return classId;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getClassName() {
        return className;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public TeacherAssignment.TeacherRole getRole() {
        return role;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * 是否是班主任任命
     */
    public boolean isHeadTeacherAssignment() {
        return role == TeacherAssignment.TeacherRole.HEAD_TEACHER;
    }
}
