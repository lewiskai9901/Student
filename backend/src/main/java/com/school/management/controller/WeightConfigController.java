package com.school.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.entity.ClassWeightConfig;
import com.school.management.mapper.ClassWeightConfigMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 班级加权配置控制器
 *
 * @author system
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/quantification/weight-config")
@RequiredArgsConstructor
@Tag(name = "加权配置管理", description = "班级加权配置相关接口")
public class WeightConfigController {

    private final ClassWeightConfigMapper weightConfigMapper;

    /**
     * 查询加权配置列表(分页)
     */
    @GetMapping("/configs")
    @Operation(summary = "查询加权配置", description = "分页查询加权配置列表")
    @PreAuthorize("hasAuthority('quantification:weight:config')")
    public Result<IPage<ClassWeightConfig>> listConfigs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "学期ID") @RequestParam(required = false) Long semesterId,
            @Parameter(description = "是否启用") @RequestParam(required = false) Integer enableWeight) {

        log.info("查询加权配置列表: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<ClassWeightConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ClassWeightConfig> wrapper = new LambdaQueryWrapper<>();

        if (semesterId != null) {
            wrapper.eq(ClassWeightConfig::getSemesterId, semesterId);
        }
        if (enableWeight != null) {
            wrapper.eq(ClassWeightConfig::getEnableWeight, enableWeight);
        }

        wrapper.orderByDesc(ClassWeightConfig::getCreatedAt);

        IPage<ClassWeightConfig> result = weightConfigMapper.selectPage(page, wrapper);
        return Result.success(result);
    }

    /**
     * 获取加权配置详情
     */
    @GetMapping("/configs/{id}")
    @Operation(summary = "获取配置详情", description = "根据ID获取加权配置详情")
    @PreAuthorize("hasAuthority('quantification:weight:config')")
    public Result<ClassWeightConfig> getConfig(@Parameter(description = "配置ID") @PathVariable Long id) {
        ClassWeightConfig config = weightConfigMapper.selectById(id);
        return Result.success(config);
    }

    /**
     * 创建加权配置
     */
    @PostMapping("/configs")
    @Operation(summary = "创建配置", description = "创建新的加权配置")
    @PreAuthorize("hasAuthority('quantification:weight:config')")
    public Result<Long> createConfig(@RequestBody ClassWeightConfig config) {
        log.info("创建加权配置: {}", config.getConfigName());

        // 检查配置名称是否重复
        LambdaQueryWrapper<ClassWeightConfig> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(ClassWeightConfig::getConfigName, config.getConfigName())
                .eq(ClassWeightConfig::getDeleted, 0);
        Long count = weightConfigMapper.selectCount(nameWrapper);
        if (count > 0) {
            log.warn("配置名称已存在: {}", config.getConfigName());
            return Result.error("配置名称已存在,请使用其他名称");
        }

        // 自动生成config_code(如果没有提供)
        if (config.getConfigCode() == null || config.getConfigCode().trim().isEmpty()) {
            config.setConfigCode("WC_" + System.currentTimeMillis());
        }

        // 处理JSON字段: 空字符串转为null,避免MySQL JSON字段报错
        if (config.getSegmentRules() != null && config.getSegmentRules().trim().isEmpty()) {
            config.setSegmentRules(null);
        }
        if (config.getCustomStandardRules() != null && config.getCustomStandardRules().trim().isEmpty()) {
            config.setCustomStandardRules(null);
        }
        if (config.getRangeDepartments() != null && config.getRangeDepartments().trim().isEmpty()) {
            config.setRangeDepartments(null);
        }
        if (config.getRangeGrades() != null && config.getRangeGrades().trim().isEmpty()) {
            config.setRangeGrades(null);
        }
        if (config.getRangeClasses() != null && config.getRangeClasses().trim().isEmpty()) {
            config.setRangeClasses(null);
        }

        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        weightConfigMapper.insert(config);
        return Result.success(config.getId());
    }

    /**
     * 更新加权配置
     */
    @PutMapping("/configs/{id}")
    @Operation(summary = "更新配置", description = "更新加权配置信息")
    @PreAuthorize("hasAuthority('quantification:weight:config')")
    public Result<Void> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @RequestBody ClassWeightConfig config) {
        log.info("更新加权配置: {}", id);

        // 检查配置名称是否与其他记录重复(排除自身)
        LambdaQueryWrapper<ClassWeightConfig> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.eq(ClassWeightConfig::getConfigName, config.getConfigName())
                .eq(ClassWeightConfig::getDeleted, 0)
                .ne(ClassWeightConfig::getId, id);
        Long count = weightConfigMapper.selectCount(nameWrapper);
        if (count > 0) {
            log.warn("配置名称已存在: {}", config.getConfigName());
            return Result.error("配置名称已存在,请使用其他名称");
        }

        config.setId(id);
        config.setUpdatedAt(LocalDateTime.now());

        // 处理空字符串，将其转换为 null（MySQL JSON 类型不接受空字符串）
        if (config.getSegmentRules() != null && config.getSegmentRules().isEmpty()) {
            config.setSegmentRules(null);
        }
        if (config.getCustomStandardRules() != null && config.getCustomStandardRules().isEmpty()) {
            config.setCustomStandardRules(null);
        }
        if (config.getRangeDepartments() != null && config.getRangeDepartments().isEmpty()) {
            config.setRangeDepartments(null);
        }
        if (config.getRangeGrades() != null && config.getRangeGrades().isEmpty()) {
            config.setRangeGrades(null);
        }
        if (config.getRangeClasses() != null && config.getRangeClasses().isEmpty()) {
            config.setRangeClasses(null);
        }
        if (config.getDescription() != null && config.getDescription().isEmpty()) {
            config.setDescription(null);
        }

        weightConfigMapper.updateById(config);
        return Result.success();
    }

    /**
     * 删除加权配置
     */
    @DeleteMapping("/configs/{id}")
    @Operation(summary = "删除配置", description = "删除加权配置")
    @PreAuthorize("hasAuthority('quantification:weight:config')")
    public Result<Void> deleteConfig(@Parameter(description = "配置ID") @PathVariable Long id) {
        log.info("删除加权配置: {}", id);
        weightConfigMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 设置默认配置
     */
    @PutMapping("/configs/{id}/default")
    @Operation(summary = "设置默认配置", description = "将指定配置设为默认")
    @PreAuthorize("hasAuthority('quantification:weight:config')")
    public Result<Void> setDefaultConfig(@Parameter(description = "配置ID") @PathVariable Long id) {
        log.info("设置默认配置: {}", id);

        // 检查配置是否存在
        ClassWeightConfig config = weightConfigMapper.selectById(id);
        if (config == null) {
            log.warn("配置不存在: {}", id);
            return Result.error("配置不存在");
        }

        // 先将所有配置的isDefault设为0
        ClassWeightConfig updateAll = new ClassWeightConfig();
        updateAll.setIsDefault(0);
        updateAll.setUpdatedAt(LocalDateTime.now());
        LambdaQueryWrapper<ClassWeightConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassWeightConfig::getDeleted, 0);
        weightConfigMapper.update(updateAll, wrapper);

        // 将指定配置的isDefault设为1
        config.setIsDefault(1);
        config.setUpdatedAt(LocalDateTime.now());
        weightConfigMapper.updateById(config);

        log.info("成功设置默认配置: {}", id);
        return Result.success();
    }
}
