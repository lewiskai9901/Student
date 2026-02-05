package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.Map;

/**
 * 更新计算规则命令
 */
@Data
public class UpdateCalculationRuleCommand {
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
