package com.school.management.controller.analysis;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisResultDTO;
import com.school.management.entity.analysis.AnalysisSnapshot;
import com.school.management.mapper.analysis.AnalysisSnapshotMapper;
import com.school.management.service.analysis.StatAnalysisConfigService;
import com.school.management.service.analysis.AnalysisExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 分析配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/analysis/configs")
@RequiredArgsConstructor
@Tag(name = "分析配置管理", description = "统计分析配置相关接口")
public class AnalysisConfigController {

    private final StatAnalysisConfigService configService;
    private final AnalysisExecutionService executionService;
    private final AnalysisSnapshotMapper snapshotMapper;

    // ==================== 配置管理 ====================

    @GetMapping
    @Operation(summary = "分页查询配置")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<IPage<AnalysisConfigDTO>> getConfigPage(
            @Parameter(description = "检查计划ID") @RequestParam(required = false) Long planId,
            @Parameter(description = "配置名称") @RequestParam(required = false) String configName,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean isEnabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int pageSize) {

        IPage<AnalysisConfigDTO> page = configService.getConfigPage(planId, configName, isEnabled, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/plan/{planId}")
    @Operation(summary = "获取计划下的配置列表")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<List<AnalysisConfigDTO>> getConfigsByPlanId(
            @Parameter(description = "检查计划ID") @PathVariable Long planId) {

        List<AnalysisConfigDTO> configs = configService.getConfigsByPlanId(planId);
        return Result.success(configs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取配置详情")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<AnalysisConfigDTO> getConfigDetail(
            @Parameter(description = "配置ID") @PathVariable Long id) {

        AnalysisConfigDTO config = configService.getConfigDetail(id);
        return Result.success(config);
    }

    @PostMapping
    @Operation(summary = "创建配置")
    @PreAuthorize("hasAuthority('quantification:statistics:edit')")
    public Result<AnalysisConfigDTO> createConfig(
            @Valid @RequestBody AnalysisConfigDTO dto) {

        AnalysisConfigDTO created = configService.createConfig(dto);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新配置")
    @PreAuthorize("hasAuthority('quantification:statistics:edit')")
    public Result<AnalysisConfigDTO> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Valid @RequestBody AnalysisConfigDTO dto) {

        AnalysisConfigDTO updated = configService.updateConfig(id, dto);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除配置")
    @PreAuthorize("hasAuthority('quantification:statistics:edit')")
    public Result<Void> deleteConfig(
            @Parameter(description = "配置ID") @PathVariable Long id) {

        configService.deleteConfig(id);
        return Result.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用配置")
    @PreAuthorize("hasAuthority('quantification:statistics:edit')")
    public Result<Void> toggleEnabled(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {

        configService.toggleEnabled(id, enabled);
        return Result.success();
    }

    @PutMapping("/{id}/default")
    @Operation(summary = "设为默认配置")
    @PreAuthorize("hasAuthority('quantification:statistics:edit')")
    public Result<Void> setDefault(
            @Parameter(description = "配置ID") @PathVariable Long id) {

        configService.setDefault(id);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制配置")
    @PreAuthorize("hasAuthority('quantification:statistics:edit')")
    public Result<AnalysisConfigDTO> copyConfig(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Parameter(description = "新名称") @RequestParam(required = false) String newName) {

        AnalysisConfigDTO copied = configService.copyConfig(id, newName);
        return Result.success(copied);
    }

    // ==================== 分析执行 ====================

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行分析")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<AnalysisResultDTO> executeAnalysis(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @Parameter(description = "是否保存快照") @RequestParam(defaultValue = "false") boolean saveSnapshot) {

        AnalysisResultDTO result = executionService.executeAnalysis(id, saveSnapshot);
        return Result.success(result);
    }

    @GetMapping("/{id}/latest-snapshot")
    @Operation(summary = "获取最新快照")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<AnalysisResultDTO> getLatestSnapshot(
            @Parameter(description = "配置ID") @PathVariable Long id) {

        AnalysisResultDTO result = executionService.getLatestSnapshot(id);
        return Result.success(result);
    }

    @GetMapping("/{id}/snapshots")
    @Operation(summary = "获取快照列表")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<List<AnalysisSnapshot>> getSnapshots(
            @Parameter(description = "配置ID") @PathVariable Long id) {

        List<AnalysisSnapshot> snapshots = snapshotMapper.selectByConfigId(id);
        return Result.success(snapshots);
    }

    @GetMapping("/snapshots/{snapshotId}")
    @Operation(summary = "获取快照详情")
    @PreAuthorize("hasAuthority('quantification:statistics:view')")
    public Result<AnalysisResultDTO> getSnapshotDetail(
            @Parameter(description = "快照ID") @PathVariable Long snapshotId) {

        AnalysisResultDTO result = executionService.getSnapshot(snapshotId);
        return Result.success(result);
    }
}
