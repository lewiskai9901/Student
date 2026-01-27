package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学任务持久化对象
 */
@Data
@TableName("teaching_tasks")
public class TeachingTaskPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long semesterId;

    private Long courseId;

    private Long classId;

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

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
