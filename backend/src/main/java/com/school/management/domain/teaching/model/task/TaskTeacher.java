package com.school.management.domain.teaching.model.task;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TaskTeacher {
    private Long id;
    private Long taskId;
    private Long teacherId;
    private Integer teacherRole; // 1=主讲, 2=辅讲
    private Integer weeklyHours; // 该教师承担的周课时数
    private BigDecimal workloadRatio;
    private String remark;

    protected TaskTeacher() {}

    public static TaskTeacher create(Long taskId, Long teacherId, int teacherRole, Integer weeklyHours) {
        TaskTeacher tt = new TaskTeacher();
        tt.taskId = taskId;
        tt.teacherId = teacherId;
        tt.teacherRole = teacherRole;
        tt.weeklyHours = weeklyHours;
        tt.workloadRatio = BigDecimal.ONE;
        return tt;
    }

    public static TaskTeacher reconstruct(Long id, Long taskId, Long teacherId,
            Integer teacherRole, Integer weeklyHours, BigDecimal workloadRatio, String remark) {
        TaskTeacher tt = new TaskTeacher();
        tt.id = id;
        tt.taskId = taskId;
        tt.teacherId = teacherId;
        tt.teacherRole = teacherRole;
        tt.weeklyHours = weeklyHours;
        tt.workloadRatio = workloadRatio;
        tt.remark = remark;
        return tt;
    }
}
