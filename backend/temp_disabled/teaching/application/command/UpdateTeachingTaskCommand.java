package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 更新教学任务命令
 */
@Data
@Builder
public class UpdateTeachingTaskCommand {

    @NotNull(message = "任务ID不能为空")
    private Long id;

    private Long classroomId;

    private Integer weeklyHours;

    private Integer startWeek;

    private Integer endWeek;

    private String examType;

    private String remark;

    /**
     * 状态: 0-待分配 1-已分配 2-进行中 3-已完成
     */
    private Integer status;

    /**
     * 教师列表(如果提供则全量更新)
     */
    private List<TaskTeacherItem> teachers;

    private Long operatorId;

    @Data
    @Builder
    public static class TaskTeacherItem {
        private Long teacherId;
        private Boolean isMain;
        private String teachingContent;
    }
}
