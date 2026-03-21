package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评选批次结果持久化对象
 */
@Data
@TableName("eval_results")
public class EvalResultPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long batchId;
    private Long campaignId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private Integer levelNum;
    private String levelName;
    private Integer rankNo;
    private BigDecimal score;
    private String conditionDetails;
    private String upgradeHint;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
