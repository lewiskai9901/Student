package com.school.management.domain.teaching.model.offering;

import com.school.management.domain.shared.Entity;
import lombok.Getter;

@Getter
public class ClassCourseAssignment implements Entity<Long> {
    private Long id;
    private Long semesterId;
    private Long classId;
    private Long offeringId;
    private Long courseId;
    private Integer weeklyHours;
    private Integer studentCount;
    private Integer status; // 0pending 1confirmed

    protected ClassCourseAssignment() {}

    @Override
    public Long getId() { return id; }

    public static ClassCourseAssignment create(Long semesterId, Long classId, Long offeringId,
            Long courseId, Integer weeklyHours, Integer studentCount) {
        ClassCourseAssignment a = new ClassCourseAssignment();
        a.semesterId = semesterId;
        a.classId = classId;
        a.offeringId = offeringId;
        a.courseId = courseId;
        a.weeklyHours = weeklyHours;
        a.studentCount = studentCount;
        a.status = 0;
        return a;
    }

    public static ClassCourseAssignment reconstruct(Long id, Long semesterId, Long classId,
            Long offeringId, Long courseId, Integer weeklyHours, Integer studentCount, Integer status) {
        ClassCourseAssignment a = new ClassCourseAssignment();
        a.id = id;
        a.semesterId = semesterId;
        a.classId = classId;
        a.offeringId = offeringId;
        a.courseId = courseId;
        a.weeklyHours = weeklyHours;
        a.studentCount = studentCount;
        a.status = status;
        return a;
    }

    public void confirm() { this.status = 1; }
    public void updateHours(Integer weeklyHours) { this.weeklyHours = weeklyHours; }
}
