package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学任务教师持久化对象
 */
@Data
@TableName("teaching_task_teachers")
public class TaskTeacherPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private Long teacherId;

    /**
     * 是否主讲
     */
    private Boolean isMain;

    /**
     * 教学内容
     */
    private String teachingContent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
