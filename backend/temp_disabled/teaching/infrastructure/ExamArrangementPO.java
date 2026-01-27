package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考试安排持久化对象
 */
@Data
@TableName("exam_arrangements")
public class ExamArrangementPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long batchId;

    private Long courseId;

    private LocalDate examDate;

    private LocalTime startTime;

    private LocalTime endTime;

    /**
     * 状态: 0-待考 1-进行中 2-已完成
     */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
