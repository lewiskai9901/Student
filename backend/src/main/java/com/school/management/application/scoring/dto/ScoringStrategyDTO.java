package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 计分策略DTO
 */
@Data
public class ScoringStrategyDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String category;
    private String categoryName;
    private String formulaTemplate;
    private String formulaDescription;
    private Map<String, Object> parametersSchema;
    private Map<String, Object> defaultParameters;
    private List<String> supportedInputTypes;
    private List<String> supportedRuleTypes;
    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;
}
