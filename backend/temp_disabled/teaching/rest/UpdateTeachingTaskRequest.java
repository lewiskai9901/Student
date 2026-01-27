package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.util.List;

/**
 * 更新教学任务请求
 */
@Data
public class UpdateTeachingTaskRequest {

    private Long classroomId;

    private Integer weeklyHours;

    private Integer startWeek;

    private Integer endWeek;

    private String examType;

    private String remark;

    private Integer status;

    private List<TaskTeacherItem> teachers;

    @Data
    public static class TaskTeacherItem {
        private Long teacherId;
        private Boolean isMain;
        private String teachingContent;
    }
}
