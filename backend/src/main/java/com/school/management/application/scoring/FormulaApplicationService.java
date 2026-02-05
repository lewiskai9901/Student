package com.school.management.application.scoring;

import com.school.management.application.scoring.dto.*;
import com.school.management.domain.scoring.model.entity.FormulaFunction;
import com.school.management.domain.scoring.model.entity.FormulaVariable;
import com.school.management.domain.scoring.repository.FormulaFunctionRepository;
import com.school.management.domain.scoring.repository.FormulaVariableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公式资源应用服务
 * 提供内置函数和变量的查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormulaApplicationService {

    private final FormulaFunctionRepository functionRepository;
    private final FormulaVariableRepository variableRepository;

    /**
     * 获取所有可用的公式资源（函数+变量）
     */
    public Map<String, Object> getAllFormulaResources() {
        Map<String, Object> resources = new HashMap<>();
        resources.put("functions", getAllFunctionsGroupedByCategory());
        resources.put("variables", getAllVariablesGroupedByCategory());
        return resources;
    }

    /**
     * 获取所有内置函数（按分类分组）
     */
    public Map<String, List<FormulaFunctionDTO>> getAllFunctionsGroupedByCategory() {
        List<FormulaFunction> functions = functionRepository.findAllEnabled();
        return functions.stream()
                .map(this::toFunctionDTO)
                .collect(Collectors.groupingBy(FormulaFunctionDTO::getCategory));
    }

    /**
     * 获取所有内置函数
     */
    public List<FormulaFunctionDTO> getAllFunctions() {
        return functionRepository.findAll().stream()
                .map(this::toFunctionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按分类获取函数
     */
    public List<FormulaFunctionDTO> getFunctionsByCategory(String category) {
        return functionRepository.findByCategory(category).stream()
                .map(this::toFunctionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据名称获取函数
     */
    public FormulaFunctionDTO getFunctionByName(String name) {
        return functionRepository.findByName(name)
                .map(this::toFunctionDTO)
                .orElseThrow(() -> new RuntimeException("函数不存在: " + name));
    }

    /**
     * 获取所有内置变量（按分类分组）
     */
    public Map<String, List<FormulaVariableDTO>> getAllVariablesGroupedByCategory() {
        List<FormulaVariable> variables = variableRepository.findAllEnabled();
        return variables.stream()
                .map(this::toVariableDTO)
                .collect(Collectors.groupingBy(FormulaVariableDTO::getCategory));
    }

    /**
     * 获取所有内置变量
     */
    public List<FormulaVariableDTO> getAllVariables() {
        return variableRepository.findAll().stream()
                .map(this::toVariableDTO)
                .collect(Collectors.toList());
    }

    /**
     * 按分类获取变量
     */
    public List<FormulaVariableDTO> getVariablesByCategory(String category) {
        return variableRepository.findByCategory(category).stream()
                .map(this::toVariableDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据名称获取变量
     */
    public FormulaVariableDTO getVariableByName(String name) {
        return variableRepository.findByName(name)
                .map(this::toVariableDTO)
                .orElseThrow(() -> new RuntimeException("变量不存在: " + name));
    }

    /**
     * 验证公式语法
     * @param formula 公式字符串
     * @return 验证结果
     */
    public FormulaValidationResult validateFormula(String formula) {
        FormulaValidationResult result = new FormulaValidationResult();
        result.setFormula(formula);

        if (formula == null || formula.trim().isEmpty()) {
            result.setValid(false);
            result.setErrorMessage("公式不能为空");
            return result;
        }

        // 基本语法检查
        try {
            // 检查括号匹配
            int parenCount = 0;
            for (char c : formula.toCharArray()) {
                if (c == '(') parenCount++;
                if (c == ')') parenCount--;
                if (parenCount < 0) {
                    result.setValid(false);
                    result.setErrorMessage("括号不匹配：多余的右括号");
                    return result;
                }
            }
            if (parenCount != 0) {
                result.setValid(false);
                result.setErrorMessage("括号不匹配：缺少右括号");
                return result;
            }

            // 提取使用的函数和变量
            List<String> usedFunctions = extractFunctions(formula);
            List<String> usedVariables = extractVariables(formula);

            result.setUsedFunctions(usedFunctions);
            result.setUsedVariables(usedVariables);

            // 检查函数是否存在
            List<String> enabledFunctions = functionRepository.findAllEnabled()
                    .stream().map(FormulaFunction::getName).collect(Collectors.toList());
            for (String fn : usedFunctions) {
                if (!enabledFunctions.contains(fn)) {
                    result.setValid(false);
                    result.setErrorMessage("未知函数: " + fn);
                    return result;
                }
            }

            // 检查变量是否存在
            List<String> enabledVariables = variableRepository.findAllEnabled()
                    .stream().map(FormulaVariable::getName).collect(Collectors.toList());
            // 添加系统保留变量
            enabledVariables.add("input");
            enabledVariables.add("value");
            enabledVariables.add("score");
            enabledVariables.add("result");

            for (String var : usedVariables) {
                if (!enabledVariables.contains(var)) {
                    result.setValid(false);
                    result.setErrorMessage("未知变量: " + var);
                    return result;
                }
            }

            result.setValid(true);
        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("公式解析错误: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从公式中提取函数名
     */
    private List<String> extractFunctions(String formula) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([A-Z_]+)\\s*\\(");
        java.util.regex.Matcher matcher = pattern.matcher(formula);
        List<String> functions = new java.util.ArrayList<>();
        while (matcher.find()) {
            functions.add(matcher.group(1));
        }
        return functions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 从公式中提取变量名
     */
    private List<String> extractVariables(String formula) {
        // 移除函数调用
        String cleaned = formula.replaceAll("[A-Z_]+\\s*\\(", "(");
        // 提取标识符
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_]*)");
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);
        List<String> variables = new java.util.ArrayList<>();
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }
        return variables.stream().distinct().collect(Collectors.toList());
    }

    private FormulaFunctionDTO toFunctionDTO(FormulaFunction fn) {
        FormulaFunctionDTO dto = new FormulaFunctionDTO();
        dto.setId(fn.getId());
        dto.setName(fn.getName());
        dto.setDescription(fn.getDescription());
        dto.setCategory(fn.getCategory());
        dto.setParametersDef(fn.getParametersDef());
        dto.setReturnType(fn.getReturnType());
        dto.setImplementation(fn.getImplementation());
        dto.setExamples(fn.getExamples());
        dto.setIsSystem(fn.getIsSystem());
        dto.setIsEnabled(fn.getIsEnabled());
        return dto;
    }

    private FormulaVariableDTO toVariableDTO(FormulaVariable var) {
        FormulaVariableDTO dto = new FormulaVariableDTO();
        dto.setId(var.getId());
        dto.setName(var.getName());
        dto.setDescription(var.getDescription());
        dto.setCategory(var.getCategory());
        dto.setValueType(var.getValueType());
        dto.setDefaultValue(var.getDefaultValue());
        dto.setSourceDescription(var.getSourceDescription());
        dto.setIsSystem(var.getIsSystem());
        dto.setIsEnabled(var.getIsEnabled());
        return dto;
    }
}
