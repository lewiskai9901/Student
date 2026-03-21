package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评选执行批次持久化对象
 */
@Data
@TableName("eval_batches")
public class EvalBatchPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long campaignId;
    private LocalDate cycleStart;
    private LocalDate cycleEnd;
    private Integer totalTargets;
    private LocalDateTime executedAt;
    private Long executedBy;
    private String status;
    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
