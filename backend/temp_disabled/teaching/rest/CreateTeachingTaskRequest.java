package com.school.management.interfaces.rest.teaching;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建教学任务请求
 */
@Data
public class CreateTeachingTaskRequest {

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @NotNull(message = "班级ID不能为空")
    private Long classId;

    private Long classroomId;

    private Integer weeklyHours;

    private Integer startWeek;

    private Integer endWeek;

    private String examType;

    private String remark;

    private List<TaskTeacherItem> teachers;

    @Data
    public static class TaskTeacherItem {
        @NotNull(message = "教师ID不能为空")
        private Long teacherId;
        private Boolean isMain;
        private String teachingContent;
    }
}
