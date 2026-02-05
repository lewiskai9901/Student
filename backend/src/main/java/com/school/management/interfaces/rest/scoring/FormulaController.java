package com.school.management.interfaces.rest.scoring;

import com.school.management.application.scoring.FormulaApplicationService;
import com.school.management.application.scoring.dto.*;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公式资源控制器
 */
@Tag(name = "公式资源管理", description = "内置函数和变量的查询")
@RestController
@RequestMapping("/scoring/formula")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaApplicationService service;

    @Operation(summary = "获取所有公式资源（函数+变量）")
    @GetMapping("/resources")
    public Result<Map<String, Object>> getAllResources() {
        return Result.success(service.getAllFormulaResources());
    }

    @Operation(summary = "获取所有内置函数（按分类分组）")
    @GetMapping("/functions/grouped")
    public Result<Map<String, List<FormulaFunctionDTO>>> getAllFunctionsGrouped() {
        return Result.success(service.getAllFunctionsGroupedByCategory());
    }

    @Operation(summary = "获取所有内置函数")
    @GetMapping("/functions")
    public Result<List<FormulaFunctionDTO>> getAllFunctions() {
        return Result.success(service.getAllFunctions());
    }

    @Operation(summary = "按分类获取函数")
    @GetMapping("/functions/category/{category}")
    public Result<List<FormulaFunctionDTO>> getFunctionsByCategory(@PathVariable String category) {
        return Result.success(service.getFunctionsByCategory(category));
    }

    @Operation(summary = "根据名称获取函数")
    @GetMapping("/functions/{name}")
    public Result<FormulaFunctionDTO> getFunctionByName(@PathVariable String name) {
        return Result.success(service.getFunctionByName(name));
    }

    @Operation(summary = "获取所有内置变量（按分类分组）")
    @GetMapping("/variables/grouped")
    public Result<Map<String, List<FormulaVariableDTO>>> getAllVariablesGrouped() {
        return Result.success(service.getAllVariablesGroupedByCategory());
    }

    @Operation(summary = "获取所有内置变量")
    @GetMapping("/variables")
    public Result<List<FormulaVariableDTO>> getAllVariables() {
        return Result.success(service.getAllVariables());
    }

    @Operation(summary = "按分类获取变量")
    @GetMapping("/variables/category/{category}")
    public Result<List<FormulaVariableDTO>> getVariablesByCategory(@PathVariable String category) {
        return Result.success(service.getVariablesByCategory(category));
    }

    @Operation(summary = "根据名称获取变量")
    @GetMapping("/variables/{name}")
    public Result<FormulaVariableDTO> getVariableByName(@PathVariable String name) {
        return Result.success(service.getVariableByName(name));
    }

    @Operation(summary = "验证公式语法")
    @PostMapping("/validate")
    public Result<FormulaValidationResult> validateFormula(@RequestBody ValidateFormulaRequest request) {
        return Result.success(service.validateFormula(request.getFormula()));
    }
}
