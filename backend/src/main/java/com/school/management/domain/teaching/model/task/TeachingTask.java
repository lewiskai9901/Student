package com.school.management.domain.teaching.model.task;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TeachingTask extends AggregateRoot<Long> {
    private String taskCode;
    private Long semesterId;
    private Long courseId;
    private Long orgUnitId;
    private Long offeringId;
    private Long teachingClassId;
    private Long orgUnitId;
    private Integer studentCount;
    private Integer weeklyHours;
    private Integer totalHours;
    private Integer startWeek;
    private Integer endWeek;
    private SchedulingStatus schedulingStatus;
    private TaskStatus taskStatus;
    private String remark;
    private Long createdBy;
    private List<TaskTeacher> teachers = new ArrayList<>();

    protected TeachingTask() {}

    public static TeachingTask create(String taskCode, Long semesterId, Long courseId,
            Long orgUnitId, Long orgUnitId, Integer studentCount, Integer weeklyHours,
            Integer totalHours, Integer startWeek, Integer endWeek,
            TaskStatus taskStatus, String remark, Long createdBy) {
        TeachingTask t = new TeachingTask();
        t.taskCode = taskCode;
        t.semesterId = semesterId;
        t.courseId = courseId;
        t.orgUnitId = orgUnitId;
        t.orgUnitId = orgUnitId;
        t.studentCount = studentCount != null ? studentCount : 0;
        t.weeklyHours = weeklyHours;
        t.totalHours = totalHours;
        t.startWeek = startWeek != null ? startWeek : 1;
        t.endWeek = endWeek != null ? endWeek : 16;
        t.schedulingStatus = SchedulingStatus.UNSCHEDULED;
        t.taskStatus = taskStatus != null ? taskStatus : TaskStatus.CONFIRMED;
        t.remark = remark;
        t.createdBy = createdBy;
        return t;
    }

    public static TeachingTask reconstruct(Long id, String taskCode, Long semesterId,
            Long courseId, Long orgUnitId, Long orgUnitId, Integer studentCount,
            Integer weeklyHours, Integer totalHours, Integer startWeek, Integer endWeek,
            SchedulingStatus schedulingStatus, TaskStatus taskStatus,
            String remark, Long createdBy) {
        TeachingTask t = new TeachingTask();
        t.id = id;
        t.taskCode = taskCode;
        t.semesterId = semesterId;
        t.courseId = courseId;
        t.orgUnitId = orgUnitId;
        t.orgUnitId = orgUnitId;
        t.studentCount = studentCount;
        t.weeklyHours = weeklyHours;
        t.totalHours = totalHours;
        t.startWeek = startWeek;
        t.endWeek = endWeek;
        t.schedulingStatus = schedulingStatus;
        t.taskStatus = taskStatus;
        t.remark = remark;
        t.createdBy = createdBy;
        return t;
    }

    public void update(Long courseId, Long orgUnitId, Long orgUnitId,
            Integer studentCount, Integer weeklyHours, Integer totalHours,
            Integer startWeek, Integer endWeek, String remark) {
        this.courseId = courseId;
        this.orgUnitId = orgUnitId;
        this.orgUnitId = orgUnitId;
        this.studentCount = studentCount;
        this.weeklyHours = weeklyHours;
        this.totalHours = totalHours;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.remark = remark;
    }

    public void updateStatus(TaskStatus status) {
        this.taskStatus = status;
    }

    public void setTeachers(List<TaskTeacher> teachers) {
        this.teachers = teachers != null ? teachers : new ArrayList<>();
    }
}
