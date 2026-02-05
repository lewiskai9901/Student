package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计算规则持久化对象
 */
@Data
@TableName("calculation_rules")
public class CalculationRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private String ruleType;

    private String conditionFormula;

    private String actionFormula;

    /**
     * JSON格式存储
     */
    private String parametersSchema;

    private String defaultParameters;

    private Integer priority;

    private Boolean stopOnMatch;

    private Boolean isSystem;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
