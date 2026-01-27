package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 创建教学任务命令
 */
@Data
@Builder
public class CreateTeachingTaskCommand {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    /**
     * 教室ID
     */
    private Long classroomId;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 起始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 考核方式
     */
    private String examType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 教师列表
     */
    private List<TaskTeacherItem> teachers;

    /**
     * 操作人
     */
    private Long operatorId;

    @Data
    @Builder
    public static class TaskTeacherItem {
        /**
         * 教师用户ID
         */
        @NotNull(message = "教师ID不能为空")
        private Long teacherId;

        /**
         * 是否主讲
         */
        private Boolean isMain;

        /**
         * 教学内容(理论/实验/实践)
         */
        private String teachingContent;
    }
}
