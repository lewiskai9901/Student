package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.Map;

/**
 * 计算规则DTO
 */
@Data
public class CalculationRuleDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String ruleType;
    private String ruleTypeName;
    private String conditionFormula;
    private String actionFormula;
    private Map<String, Object> parametersSchema;
    private Map<String, Object> defaultParameters;
    private Integer priority;
    private Boolean stopOnMatch;
    private Boolean isSystem;
    private Boolean isEnabled;
}
