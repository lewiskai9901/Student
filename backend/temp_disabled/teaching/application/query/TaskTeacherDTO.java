package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

/**
 * 教学任务教师DTO
 */
@Data
@Builder
public class TaskTeacherDTO {

    private Long id;

    private Long taskId;

    private Long teacherId;

    private String teacherName;

    private String teacherCode;

    /**
     * 是否主讲
     */
    private Boolean isMain;

    /**
     * 教学内容
     */
    private String teachingContent;
}
