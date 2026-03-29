package com.school.management.domain.teaching.model.teachingclass;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class TeachingClass extends AggregateRoot<Long> {
    private Long semesterId;
    private String className;
    private String classCode;
    private Long courseId;
    private TeachingClassType classType;
    private Integer weeklyHours;
    private Integer studentCount;
    private String requiredRoomType;
    private Integer requiredCapacity;
    private Integer startWeek;
    private Integer endWeek;
    private Integer status; // 1active 0inactive
    private String remark;
    private Long createdBy;
    private List<TeachingClassMember> members = new ArrayList<>();

    protected TeachingClass() {}

    public static TeachingClass create(Long semesterId, String className, Long courseId,
            TeachingClassType classType, Integer weeklyHours, Long createdBy) {
        TeachingClass tc = new TeachingClass();
        tc.semesterId = semesterId;
        tc.className = className;
        tc.courseId = courseId;
        tc.classType = classType;
        tc.weeklyHours = weeklyHours;
        tc.studentCount = 0;
        tc.startWeek = 1;
        tc.status = 1;
        tc.createdBy = createdBy;
        return tc;
    }

    public static TeachingClass reconstruct(Long id, Long semesterId, String className,
            String classCode, Long courseId, TeachingClassType classType, Integer weeklyHours,
            Integer studentCount, String requiredRoomType, Integer requiredCapacity,
            Integer startWeek, Integer endWeek, Integer status, String remark, Long createdBy) {
        TeachingClass tc = new TeachingClass();
        tc.id = id;
        tc.semesterId = semesterId;
        tc.className = className;
        tc.classCode = classCode;
        tc.courseId = courseId;
        tc.classType = classType;
        tc.weeklyHours = weeklyHours;
        tc.studentCount = studentCount;
        tc.requiredRoomType = requiredRoomType;
        tc.requiredCapacity = requiredCapacity;
        tc.startWeek = startWeek;
        tc.endWeek = endWeek;
        tc.status = status;
        tc.remark = remark;
        tc.createdBy = createdBy;
        return tc;
    }

    public void setMembers(List<TeachingClassMember> members) { this.members = members; }

    public void addAdminClass(Long adminClassId, int classStudentCount) {
        TeachingClassMember member = TeachingClassMember.ofAdminClass(this.id, adminClassId);
        this.members.add(member);
        this.studentCount = (this.studentCount != null ? this.studentCount : 0) + classStudentCount;
        this.requiredCapacity = this.studentCount;
    }

    public void addStudent(Long studentId) {
        TeachingClassMember member = TeachingClassMember.ofStudent(this.id, studentId);
        this.members.add(member);
        this.studentCount = (this.studentCount != null ? this.studentCount : 0) + 1;
    }

    public void update(String className, Integer weeklyHours, String requiredRoomType,
            Integer requiredCapacity, Integer startWeek, Integer endWeek, String remark) {
        this.className = className;
        this.weeklyHours = weeklyHours;
        this.requiredRoomType = requiredRoomType;
        this.requiredCapacity = requiredCapacity;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.remark = remark;
    }

    public void deactivate() { this.status = 0; }
    public void activate() { this.status = 1; }
}
