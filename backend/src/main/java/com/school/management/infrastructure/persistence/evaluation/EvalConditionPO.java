package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评选条件持久化对象
 */
@Data
@TableName("eval_conditions")
public class EvalConditionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long levelId;
    private String sourceType;
    private String sourceConfig;
    private String metric;
    private String operator;
    private String threshold;
    private String scope;
    private String scopeRole;
    private String timeRange;
    private Integer timeRangeDays;
    private String description;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
