package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课表条目持久化对象
 */
@Data
@TableName("schedule_entries")
public class ScheduleEntryPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long scheduleId;

    private Long taskId;

    /**
     * 星期: 1-7
     */
    private Integer weekday;

    /**
     * 节次
     */
    private Integer slot;

    /**
     * 起始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 周类型: 0-每周 1-单周 2-双周
     */
    private Integer weekType;

    /**
     * 教室ID
     */
    private Long classroomId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
