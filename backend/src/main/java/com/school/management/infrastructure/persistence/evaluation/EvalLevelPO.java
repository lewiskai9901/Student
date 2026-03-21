package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评选级别持久化对象
 */
@Data
@TableName("eval_levels")
public class EvalLevelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long campaignId;
    private Integer levelNum;
    private String levelName;
    private String conditionLogic;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
