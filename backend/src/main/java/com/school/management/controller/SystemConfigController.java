package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.annotation.OperationLog;
import com.school.management.common.result.Result;
import com.school.management.entity.SystemConfig;
import com.school.management.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/system/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 分页查询系统配置
     */
    @Operation(summary = "分页查询系统配置")
    @GetMapping
    @PreAuthorize("hasAuthority('system:config:view')")
    public Result<IPage<SystemConfig>> queryPage(
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configGroup,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        IPage<SystemConfig> page = systemConfigService.queryPage(configKey, configGroup, status, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 根据配置组查询配置
     */
    @Operation(summary = "根据配置组查询配置")
    @GetMapping("/group/{configGroup}")
    @PreAuthorize("hasAuthority('system:config:view')")
    public Result<List<SystemConfig>> getByGroup(@PathVariable String configGroup) {
        List<SystemConfig> configs = systemConfigService.getByGroup(configGroup);
        return Result.success(configs);
    }

    /**
     * 公开的API：获取系统基本配置（不需要认证）
     * 用于登录页和主界面加载系统名称、Logo等
     */
    @Operation(summary = "获取公开的系统配置")
    @GetMapping("/public/system")
    public Result<List<SystemConfig>> getPublicSystemConfig() {
        List<SystemConfig> configs = systemConfigService.getByGroup("system");
        return Result.success(configs);
    }

    /**
     * 公开的API：获取业务配置（不需要认证）
     * 用于前端加载密码长度、文件大小限制等业务规则
     */
    @Operation(summary = "获取公开的业务配置")
    @GetMapping("/public/business")
    public Result<List<SystemConfig>> getPublicBusinessConfig() {
        List<SystemConfig> configs = systemConfigService.getByGroup("business");
        return Result.success(configs);
    }

    /**
     * 公开的API：获取UI配置（不需要认证）
     * 用于前端加载分页大小、主题颜色等UI设置
     */
    @Operation(summary = "获取公开的UI配置")
    @GetMapping("/public/ui")
    public Result<List<SystemConfig>> getPublicUiConfig() {
        List<SystemConfig> configs = systemConfigService.getByGroup("ui");
        return Result.success(configs);
    }

    /**
     * 根据ID查询配置详情
     */
    @Operation(summary = "查询配置详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:view')")
    public Result<SystemConfig> getById(@PathVariable Long id) {
        SystemConfig config = systemConfigService.getById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        return Result.success(config);
    }

    /**
     * 创建配置
     */
    @Operation(summary = "创建配置")
    @PostMapping
    @PreAuthorize("hasAuthority('system:config:add')")
    @OperationLog(module = "system", type = "create", name = "创建系统配置")
    public Result<Long> create(@RequestBody SystemConfig config) {
        boolean success = systemConfigService.save(config);
        if (success) {
            systemConfigService.refreshCache();
            return Result.success(config.getId());
        }
        return Result.error("创建失败");
    }

    /**
     * 更新配置
     */
    @Operation(summary = "更新配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:edit')")
    @OperationLog(module = "system", type = "update", name = "更新系统配置")
    public Result<Void> update(@PathVariable Long id, @RequestBody SystemConfig config) {
        config.setId(id);
        boolean success = systemConfigService.updateById(config);
        if (success) {
            systemConfigService.refreshCache();
            return Result.success(null);
        }
        return Result.error("更新失败");
    }

    /**
     * 批量更新配置
     */
    @Operation(summary = "批量更新配置")
    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('system:config:edit')")
    @OperationLog(module = "system", type = "update", name = "批量更新系统配置")
    public Result<Void> batchUpdate(@RequestBody Map<String, String> configs) {
        boolean success = systemConfigService.batchUpdate(configs);
        if (success) {
            return Result.success(null);
        }
        return Result.error("批量更新失败");
    }

    /**
     * 删除配置
     */
    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除系统配置")
    public Result<Void> delete(@PathVariable Long id) {
        SystemConfig config = systemConfigService.getById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        if (config.getIsSystem() == 1) {
            return Result.error("系统内置配置不允许删除");
        }

        boolean success = systemConfigService.removeById(id);
        if (success) {
            systemConfigService.refreshCache();
            return Result.success(null);
        }
        return Result.error("删除失败");
    }

    /**
     * 刷新配置缓存
     */
    @Operation(summary = "刷新配置缓存")
    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('system:config:edit')")
    @OperationLog(module = "system", type = "update", name = "刷新配置缓存")
    public Result<Void> refreshCache() {
        systemConfigService.refreshCache();
        return Result.success(null);
    }
}
