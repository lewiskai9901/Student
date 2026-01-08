package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.DailyCheckWeightConfigDTO;
import com.school.management.service.DailyCheckWeightConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 日常检查加权配置控制器
 *
 * 支持一次检查关联多个加权配置，每个配置可应用于特定分类或扣分项
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/daily-check-weight-configs")
@Tag(name = "日常检查加权配置", description = "管理日常检查的多加权配置")
public class DailyCheckWeightConfigController {

    private final DailyCheckWeightConfigService configService;

    @GetMapping("/daily-check/{dailyCheckId}")
    @Operation(summary = "获取日常检查的所有加权配置", description = "根据日常检查ID获取所有关联的加权配置")
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public Result<List<DailyCheckWeightConfigDTO>> getConfigsByDailyCheckId(
            @Parameter(description = "日常检查ID") @PathVariable Long dailyCheckId) {
        log.info("获取日常检查的加权配置列表: dailyCheckId={}", dailyCheckId);
        List<DailyCheckWeightConfigDTO> configs = configService.getConfigsByDailyCheckId(dailyCheckId);
        return Result.success(configs);
    }

    @GetMapping("/applicable")
    @Operation(summary = "获取适用的加权配置", description = "根据分类ID和扣分项ID查询适用的加权配置")
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public Result<DailyCheckWeightConfigDTO> getApplicableConfig(
            @Parameter(description = "日常检查ID") @RequestParam Long dailyCheckId,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "扣分项ID") @RequestParam(required = false) Long itemId) {
        log.info("获取适用的加权配置: dailyCheckId={}, categoryId={}, itemId={}", dailyCheckId, categoryId, itemId);
        DailyCheckWeightConfigDTO config = configService.getApplicableConfig(dailyCheckId, categoryId, itemId);
        return Result.success(config);
    }

    @PostMapping("/daily-check/{dailyCheckId}")
    @Operation(summary = "保存日常检查的加权配置列表", description = "保存多个加权配置到日常检查")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<List<DailyCheckWeightConfigDTO>> saveConfigs(
            @Parameter(description = "日常检查ID") @PathVariable Long dailyCheckId,
            @RequestBody List<DailyCheckWeightConfigDTO> configs) {
        log.info("保存日常检查的加权配置: dailyCheckId={}, configCount={}", dailyCheckId, configs.size());
        List<DailyCheckWeightConfigDTO> savedConfigs = configService.saveConfigs(dailyCheckId, configs);
        return Result.success(savedConfigs);
    }

    @PostMapping
    @Operation(summary = "添加加权配置", description = "为日常检查添加单个加权配置")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<DailyCheckWeightConfigDTO> addConfig(@RequestBody DailyCheckWeightConfigDTO config) {
        log.info("添加加权配置: dailyCheckId={}, weightConfigId={}", config.getDailyCheckId(), config.getWeightConfigId());
        DailyCheckWeightConfigDTO savedConfig = configService.addConfig(config);
        return Result.success(savedConfig);
    }

    @PutMapping("/{configId}")
    @Operation(summary = "更新加权配置", description = "更新现有的加权配置")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<DailyCheckWeightConfigDTO> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @RequestBody DailyCheckWeightConfigDTO config) {
        log.info("更新加权配置: configId={}", configId);
        config.setId(configId);
        DailyCheckWeightConfigDTO updatedConfig = configService.updateConfig(config);
        return Result.success(updatedConfig);
    }

    @DeleteMapping("/{configId}")
    @Operation(summary = "删除加权配置", description = "删除指定的加权配置")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<Void> deleteConfig(@Parameter(description = "配置ID") @PathVariable Long configId) {
        log.info("删除加权配置: configId={}", configId);
        configService.deleteConfig(configId);
        return Result.success();
    }

    @DeleteMapping("/daily-check/{dailyCheckId}")
    @Operation(summary = "删除日常检查的所有加权配置", description = "删除指定日常检查的所有加权配置")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<Integer> deleteByDailyCheckId(@Parameter(description = "日常检查ID") @PathVariable Long dailyCheckId) {
        log.info("删除日常检查的所有加权配置: dailyCheckId={}", dailyCheckId);
        int count = configService.deleteByDailyCheckId(dailyCheckId);
        return Result.success(count);
    }

    @PostMapping("/{configId}/calculate-standard-size")
    @Operation(summary = "计算并更新标准人数", description = "根据配置的标准人数模式计算实际标准人数")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<Integer> calculateStandardSize(@Parameter(description = "配置ID") @PathVariable Long configId) {
        log.info("计算标准人数: configId={}", configId);
        Integer standardSize = configService.calculateAndUpdateStandardSize(configId);
        return Result.success(standardSize);
    }

    @GetMapping("/colors")
    @Operation(summary = "获取预定义颜色方案", description = "获取可用的颜色方案列表")
    public Result<List<DailyCheckWeightConfigDTO.ColorScheme>> getPredefinedColors() {
        return Result.success(configService.getPredefinedColors());
    }
}
