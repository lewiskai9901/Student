package com.school.management.controller.rating;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.rating.*;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.rating.RatingConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评级配置控制器
 *
 * @author System
 * @since 4.4.0
 */
@Tag(name = "评级配置管理")
@RestController
@RequestMapping("/quantification/rating/config")
@RequiredArgsConstructor
public class RatingConfigController {

    private final RatingConfigService ratingConfigService;

    @Operation(summary = "创建评级配置")
    @PostMapping
    @PreAuthorize("hasAuthority('quantification:config:add')")
    public Result<Long> createConfig(
            @Validated @RequestBody RatingConfigCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long configId = ratingConfigService.createConfig(dto, userDetails.getUserId());
        return Result.success(configId);
    }

    @Operation(summary = "更新评级配置")
    @PutMapping
    @PreAuthorize("hasAuthority('quantification:config:edit')")
    public Result<Void> updateConfig(
            @Validated @RequestBody RatingConfigUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ratingConfigService.updateConfig(dto, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "删除评级配置")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:config:delete')")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        ratingConfigService.deleteConfig(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用评级配置")
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('quantification:config:edit')")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        ratingConfigService.toggleEnabled(id, enabled);
        return Result.success();
    }

    @Operation(summary = "获取评级配置详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<RatingConfigVO> getConfigDetail(@PathVariable Long id) {
        RatingConfigVO config = ratingConfigService.getConfigDetail(id);
        return Result.success(config);
    }

    @Operation(summary = "分页查询评级配置")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<Page<RatingConfigVO>> getConfigPage(@Validated RatingConfigQueryDTO query) {
        Page<RatingConfigVO> page = ratingConfigService.getConfigPage(query);
        return Result.success(page);
    }

    @Operation(summary = "获取检查计划的所有评级配置")
    @GetMapping("/plan/{checkPlanId}")
    @PreAuthorize("hasAuthority('quantification:config:view')")
    public Result<List<RatingConfigVO>> getConfigsByPlan(@PathVariable Long checkPlanId) {
        List<RatingConfigVO> configs = ratingConfigService.getConfigsByPlan(checkPlanId);
        return Result.success(configs);
    }
}
