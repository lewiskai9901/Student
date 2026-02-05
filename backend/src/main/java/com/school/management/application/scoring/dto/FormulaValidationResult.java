package com.school.management.application.scoring.dto;

import lombok.Data;
import java.util.List;

/**
 * 公式验证结果
 */
@Data
public class FormulaValidationResult {
    private String formula;
    private boolean valid;
    private String errorMessage;
    private List<String> usedFunctions;
    private List<String> usedVariables;
}
