package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.service.CheckRecordWeightConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 加权配置管理控制器 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Slf4j
@RestController
@RequestMapping("/api/quantification/weight-configs")
@RequiredArgsConstructor
@Tag(name = "加权配置管理", description = "检查记录加权配置管理相关接口 (V3.0)")
public class WeightConfigManageController {

    private final CheckRecordWeightConfigService weightConfigService;

    /**
     * 获取配置树（3级结构）
     */
    @GetMapping("/tree/{recordId}")
    @Operation(summary = "获取配置树", description = "获取检查记录的3级配置树（RECORD→CATEGORY→ITEM）")
    @PreAuthorize("hasAuthority('quantification:weight-config:view')")
    public Result<List<Map<String, Object>>> getConfigTree(
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        log.info("获取配置树: recordId={}", recordId);
        List<Map<String, Object>> configTree = weightConfigService.getConfigTree(recordId);
        return Result.success(configTree);
    }

    /**
     * 获取生效的加权配置
     */
    @GetMapping("/effective")
    @Operation(summary = "获取生效配置", description = "获取生效的加权配置（处理3级继承逻辑）")
    @PreAuthorize("hasAuthority('quantification:weight-config:view')")
    public Result<Map<String, Object>> getEffectiveConfig(
            @Parameter(description = "记录ID") @RequestParam Long recordId,
            @Parameter(description = "类别ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "检查项ID") @RequestParam(required = false) Long itemId) {
        log.info("获取生效配置: recordId={}, categoryId={}, itemId={}", recordId, categoryId, itemId);
        Map<String, Object> config = weightConfigService.getEffectiveConfig(recordId, categoryId, itemId);
        return Result.success(config);
    }

    /**
     * 设置记录级别配置
     */
    @PostMapping("/record-level")
    @Operation(summary = "设置记录级配置", description = "设置记录级别的加权配置")
    @PreAuthorize("hasAuthority('quantification:weight-config:set')")
    public Result<Void> setRecordLevelConfig(@RequestBody Map<String, Object> request) {
        log.info("设置记录级别配置");
        Long recordId = ((Number) request.get("recordId")).longValue();
        Long weightConfigId = request.get("weightConfigId") != null ?
                ((Number) request.get("weightConfigId")).longValue() : null;
        Integer weightEnabled = (Integer) request.get("weightEnabled");
        weightConfigService.setRecordLevelConfig(recordId, weightConfigId, weightEnabled);
        return Result.success();
    }

    /**
     * 设置类别级别配置
     */
    @PostMapping("/category-level")
    @Operation(summary = "设置类别级配置", description = "设置类别级别的加权配置")
    @PreAuthorize("hasAuthority('quantification:weight-config:set')")
    public Result<Void> setCategoryLevelConfig(@RequestBody Map<String, Object> request) {
        log.info("设置类别级别配置");
        Long recordId = ((Number) request.get("recordId")).longValue();
        Long categoryId = ((Number) request.get("categoryId")).longValue();
        Long weightConfigId = request.get("weightConfigId") != null ?
                ((Number) request.get("weightConfigId")).longValue() : null;
        Integer isInherited = (Integer) request.get("isInherited");
        Integer weightEnabled = (Integer) request.get("weightEnabled");
        weightConfigService.setCategoryLevelConfig(recordId, categoryId, weightConfigId, isInherited, weightEnabled);
        return Result.success();
    }

    /**
     * 设置检查项级别配置
     */
    @PostMapping("/item-level")
    @Operation(summary = "设置检查项级配置", description = "设置检查项级别的加权配置")
    @PreAuthorize("hasAuthority('quantification:weight-config:set')")
    public Result<Void> setItemLevelConfig(@RequestBody Map<String, Object> request) {
        log.info("设置检查项级别配置");
        Long recordId = ((Number) request.get("recordId")).longValue();
        Long categoryId = ((Number) request.get("categoryId")).longValue();
        Long itemId = ((Number) request.get("itemId")).longValue();
        Long weightConfigId = request.get("weightConfigId") != null ?
                ((Number) request.get("weightConfigId")).longValue() : null;
        Integer isInherited = (Integer) request.get("isInherited");
        Integer weightEnabled = (Integer) request.get("weightEnabled");
        weightConfigService.setItemLevelConfig(recordId, categoryId, itemId, weightConfigId, isInherited, weightEnabled);
        return Result.success();
    }

    /**
     * 批量设置类别级别配置
     */
    @PostMapping("/category-level/batch")
    @Operation(summary = "批量设置类别配置", description = "批量设置多个类别的加权配置")
    @PreAuthorize("hasAuthority('quantification:weight-config:set')")
    public Result<Void> batchSetCategoryConfigs(@RequestBody Map<String, Object> request) {
        log.info("批量设置类别级别配置");
        Long recordId = ((Number) request.get("recordId")).longValue();
        List<Map<String, Object>> configs = (List<Map<String, Object>>) request.get("configs");
        weightConfigService.batchSetCategoryConfigs(recordId, configs);
        return Result.success();
    }

    /**
     * 批量设置检查项级别配置
     */
    @PostMapping("/item-level/batch")
    @Operation(summary = "批量设置检查项配置", description = "批量设置多个检查项的加权配置")
    @PreAuthorize("hasAuthority('quantification:weight-config:set')")
    public Result<Void> batchSetItemConfigs(@RequestBody Map<String, Object> request) {
        log.info("批量设置检查项级别配置");
        Long recordId = ((Number) request.get("recordId")).longValue();
        List<Map<String, Object>> configs = (List<Map<String, Object>>) request.get("configs");
        weightConfigService.batchSetItemConfigs(recordId, configs);
        return Result.success();
    }

    /**
     * 重置为记录级别配置
     */
    @PostMapping("/{recordId}/reset")
    @Operation(summary = "重置配置", description = "重置为记录级别配置（删除所有覆盖配置）")
    @PreAuthorize("hasAuthority('quantification:weight-config:set')")
    public Result<Void> resetToRecordLevel(@Parameter(description = "记录ID") @PathVariable Long recordId) {
        log.info("重置为记录级别配置: recordId={}", recordId);
        weightConfigService.resetToRecordLevel(recordId);
        return Result.success();
    }

    /**
     * 生成配置快照
     */
    @GetMapping("/{recordId}/snapshot")
    @Operation(summary = "生成配置快照", description = "生成加权配置快照（用于历史记录）")
    @PreAuthorize("hasAuthority('quantification:weight-config:view')")
    public Result<Map<String, Object>> generateConfigSnapshot(
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        log.info("生成配置快照: recordId={}", recordId);
        Map<String, Object> snapshot = weightConfigService.generateConfigSnapshot(recordId);
        return Result.success(snapshot);
    }
}
