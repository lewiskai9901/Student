package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.persistence.system.SystemConfigMapper;
import com.school.management.infrastructure.persistence.system.SystemConfigPO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制器
 * 提供公开和受保护的配置API
 */
@Slf4j
@RestController
@RequestMapping("/system/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理接口")
public class SystemConfigController {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 获取指定分组的公开配置（无需认证）
     * 用于前端加载系统名称、Logo等基础配置
     */
    @GetMapping("/public/{group}")
    @Operation(summary = "获取公开配置", description = "根据分组获取公开的系统配置，无需登录")
    public Result<List<SystemConfigPO>> getPublicConfigs(@PathVariable String group) {
        log.debug("获取公开配置, group={}", group);
        List<SystemConfigPO> configs = systemConfigMapper.findByGroup(group);
        return Result.success(configs);
    }

    /**
     * 获取所有配置（需要认证）
     */
    @GetMapping
    @Operation(summary = "获取所有配置", description = "获取所有系统配置，需要登录")
    public Result<List<SystemConfigPO>> getAllConfigs() {
        List<SystemConfigPO> configs = systemConfigMapper.findAllEnabled();
        return Result.success(configs);
    }

    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/key/{key}")
    @Operation(summary = "根据键获取配置", description = "根据配置键获取单个配置")
    public Result<SystemConfigPO> getConfigByKey(@PathVariable String key) {
        SystemConfigPO config = systemConfigMapper.findByKey(key);
        if (config == null) {
            return Result.error("配置不存在: " + key);
        }
        return Result.success(config);
    }
}
