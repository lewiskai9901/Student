package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考试批次持久化对象
 */
@Data
@TableName("exam_batches")
public class ExamBatchPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long semesterId;

    private String batchName;

    /**
     * 考试类型: 1-期中考试 2-期末考试 3-补考 4-结业考试
     */
    private Integer examType;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * 状态: 0-草稿 1-已发布 2-进行中 3-已结束
     */
    private Integer status;

    private String remark;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
