package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 创建计分策略命令
 */
@Data
public class CreateScoringStrategyCommand {
    private String code;
    private String name;
    private String description;
    private String category;
    private String formulaTemplate;
    private String formulaDescription;
    private Map<String, Object> parametersSchema;
    private Map<String, Object> defaultParameters;
    private List<String> supportedInputTypes;
}
