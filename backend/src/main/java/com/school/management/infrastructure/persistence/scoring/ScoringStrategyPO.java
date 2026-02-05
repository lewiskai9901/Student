package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 计分策略持久化对象
 */
@Data
@TableName("scoring_strategies")
public class ScoringStrategyPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String description;

    private String category;

    private String formulaTemplate;

    private String formulaDescription;

    /**
     * JSON格式存储
     */
    private String parametersSchema;

    private String defaultParameters;

    private String supportedInputTypes;

    private String supportedRuleTypes;

    private Boolean isSystem;

    private Boolean isEnabled;

    private Integer sortOrder;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
