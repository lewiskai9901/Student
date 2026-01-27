package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课表持久化对象
 */
@Data
@TableName("course_schedules")
public class CourseSchedulePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long semesterId;

    private String scheduleName;

    /**
     * 状态: 0-草稿 1-已发布
     */
    private Integer status;

    private Integer version;

    private LocalDateTime publishedAt;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
