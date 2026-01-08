package com.school.management.controller.evaluation;

import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.EvaluationDimension;
import com.school.management.service.evaluation.EvaluationDimensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 综测维度配置控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/dimensions")
@RequiredArgsConstructor
@Tag(name = "综测维度配置", description = "综测维度权重配置管理接口")
public class EvaluationDimensionController {

    private final EvaluationDimensionService dimensionService;

    /**
     * 获取所有启用的维度配置
     */
    @GetMapping
    @Operation(summary = "获取所有启用的维度配置")
    @PreAuthorize("hasAuthority('evaluation:config:list')")
    public Result<List<EvaluationDimension>> getAllEnabled() {
        log.info("获取所有启用的维度配置");
        List<EvaluationDimension> list = dimensionService.getAllEnabled();
        return Result.success(list);
    }

    /**
     * 根据维度编码获取配置
     */
    @GetMapping("/{code}")
    @Operation(summary = "根据维度编码获取配置")
    @PreAuthorize("hasAuthority('evaluation:config:list')")
    public Result<EvaluationDimension> getByCode(
            @Parameter(description = "维度编码") @PathVariable String code) {
        log.info("获取维度配置: code={}", code);
        EvaluationDimension dimension = dimensionService.getByCode(code);
        return Result.success(dimension);
    }

    /**
     * 更新维度配置
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新维度配置")
    @PreAuthorize("hasAuthority('evaluation:config:update')")
    public Result<Void> updateDimension(
            @Parameter(description = "维度ID") @PathVariable Long id,
            @RequestBody EvaluationDimension dimension) {
        log.info("更新维度配置: id={}", id);
        dimension.setId(id);
        dimensionService.updateDimension(dimension);
        return Result.success();
    }
}
