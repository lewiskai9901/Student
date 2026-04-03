package com.school.management.domain.teaching.model.task;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TaskTeacher {
    private Long id;
    private Long taskId;
    private Long teacherId;
    private Integer teacherRole; // 1=主讲, 2=辅讲
    private BigDecimal workloadRatio;
    private String remark;

    protected TaskTeacher() {}

    public static TaskTeacher create(Long taskId, Long teacherId, int teacherRole) {
        TaskTeacher tt = new TaskTeacher();
        tt.taskId = taskId;
        tt.teacherId = teacherId;
        tt.teacherRole = teacherRole;
        tt.workloadRatio = BigDecimal.ONE;
        return tt;
    }

    public static TaskTeacher reconstruct(Long id, Long taskId, Long teacherId,
            Integer teacherRole, BigDecimal workloadRatio, String remark) {
        TaskTeacher tt = new TaskTeacher();
        tt.id = id;
        tt.taskId = taskId;
        tt.teacherId = teacherId;
        tt.teacherRole = teacherRole;
        tt.workloadRatio = workloadRatio;
        tt.remark = remark;
        return tt;
    }
}
