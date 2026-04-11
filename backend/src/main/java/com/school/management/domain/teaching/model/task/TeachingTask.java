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
    private Integer studentCount;
    private Integer weeklyHours;
    private Integer totalHours;
    private Integer startWeek;
    private Integer endWeek;
    private String roomTypeRequired;     // 教室类型需求 (关联场所 type_code)
    private Integer consecutivePeriods;  // 连排节数 (1=不连排, 2/3/4=连排)
    private Integer courseNature;        // 课程性质 1=理论 2=实验 3=实践 4=理论+实验
    private SchedulingStatus schedulingStatus;
    private TaskStatus taskStatus;
    private String remark;
    private Long createdBy;
    private List<TaskTeacher> teachers = new ArrayList<>();

    protected TeachingTask() {}

    public static TeachingTask create(String taskCode, Long semesterId, Long courseId,
            Long orgUnitId, Integer studentCount, Integer weeklyHours,
            Integer totalHours, Integer startWeek, Integer endWeek,
            String roomTypeRequired, Integer consecutivePeriods, Integer courseNature,
            TaskStatus taskStatus, String remark, Long createdBy) {
        TeachingTask t = new TeachingTask();
        t.taskCode = taskCode;
        t.semesterId = semesterId;
        t.courseId = courseId;
        t.orgUnitId = orgUnitId;
        t.studentCount = studentCount != null ? studentCount : 0;
        t.weeklyHours = weeklyHours;
        t.totalHours = totalHours;
        t.startWeek = startWeek != null ? startWeek : 1;
        t.endWeek = endWeek != null ? endWeek : 16;
        t.roomTypeRequired = roomTypeRequired;
        t.consecutivePeriods = consecutivePeriods != null ? consecutivePeriods : 2;
        t.courseNature = courseNature != null ? courseNature : 1;
        t.schedulingStatus = SchedulingStatus.UNSCHEDULED;
        t.taskStatus = taskStatus != null ? taskStatus : TaskStatus.PENDING;
        t.remark = remark;
        t.createdBy = createdBy;
        return t;
    }

    public static TeachingTask reconstruct(Long id, String taskCode, Long semesterId,
            Long courseId, Long orgUnitId, Integer studentCount,
            Integer weeklyHours, Integer totalHours, Integer startWeek, Integer endWeek,
            String roomTypeRequired, Integer consecutivePeriods, Integer courseNature,
            SchedulingStatus schedulingStatus, TaskStatus taskStatus,
            String remark, Long createdBy) {
        TeachingTask t = new TeachingTask();
        t.id = id;
        t.taskCode = taskCode;
        t.semesterId = semesterId;
        t.courseId = courseId;
        t.orgUnitId = orgUnitId;
        t.studentCount = studentCount;
        t.weeklyHours = weeklyHours;
        t.totalHours = totalHours;
        t.startWeek = startWeek;
        t.endWeek = endWeek;
        t.roomTypeRequired = roomTypeRequired;
        t.consecutivePeriods = consecutivePeriods;
        t.courseNature = courseNature;
        t.schedulingStatus = schedulingStatus;
        t.taskStatus = taskStatus;
        t.remark = remark;
        t.createdBy = createdBy;
        return t;
    }

    public void update(Long courseId, Long orgUnitId,
            Integer studentCount, Integer weeklyHours, Integer totalHours,
            Integer startWeek, Integer endWeek,
            String roomTypeRequired, Integer consecutivePeriods, Integer courseNature,
            String remark) {
        this.courseId = courseId;
        this.orgUnitId = orgUnitId;
        this.studentCount = studentCount;
        this.weeklyHours = weeklyHours;
        this.totalHours = totalHours;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.roomTypeRequired = roomTypeRequired;
        this.consecutivePeriods = consecutivePeriods;
        this.courseNature = courseNature;
        this.remark = remark;
    }

    public void updateStatus(TaskStatus status) {
        this.taskStatus = status;
    }

    public void setTeachers(List<TaskTeacher> teachers) {
        this.teachers = teachers != null ? teachers : new ArrayList<>();
    }
}
