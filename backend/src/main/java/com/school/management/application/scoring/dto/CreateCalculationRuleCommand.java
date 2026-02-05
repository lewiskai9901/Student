package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.Map;

/**
 * 创建计算规则命令
 */
@Data
public class CreateCalculationRuleCommand {
    private String code;
    private String name;
    private String description;
    private String ruleType;
    private String conditionFormula;
    private String actionFormula;
    private Map<String, Object> parametersSchema;
    private Map<String, Object> defaultParameters;
    private Integer priority;
    private Boolean stopOnMatch;
}
