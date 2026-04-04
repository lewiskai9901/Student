package com.school.management.interfaces.rest.system;

import com.school.management.common.audit.Audited;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.system.SystemConfigMapper;
import com.school.management.infrastructure.persistence.system.SystemConfigPO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

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
     * 公开接口允许返回的安全配置键
     * 仅包含前端展示所需的基础信息，不包含任何敏感配置
     */
    private static final Set<String> PUBLIC_SAFE_KEYS = Set.of(
            "system.name", "system.version", "system.logo", "system.copyright",
            "system.description", "system.favicon"
    );

    /**
     * 公开接口允许访问的分组
     */
    private static final Set<String> PUBLIC_ALLOWED_GROUPS = Set.of("system", "ui");

    /**
     * 获取指定分组的公开配置（无需认证）
     * 仅允许 system 和 ui 分组，且 system 分组过滤为安全键
     */
    @GetMapping("/public/{group}")
    @Operation(summary = "获取公开配置", description = "根据分组获取公开的系统配置，无需登录。仅允许system和ui分组。")
    public Result<List<SystemConfigPO>> getPublicConfigs(@PathVariable String group) {
        log.debug("获取公开配置, group={}", group);
        // Only allow 'system' and 'ui' groups publicly
        if (!PUBLIC_ALLOWED_GROUPS.contains(group)) {
            return Result.success(Collections.emptyList());
        }
        List<SystemConfigPO> configs = systemConfigMapper.findByGroup(group);
        // For system group, filter to safe keys only
        if ("system".equals(group)) {
            configs.removeIf(c -> !PUBLIC_SAFE_KEYS.contains(c.getConfigKey()));
        }
        return Result.success(configs);
    }

    /**
     * 获取所有配置（需要认证）
     */
    @GetMapping
    @CasbinAccess(resource = "system:config", action = "view")
    @Operation(summary = "获取所有配置", description = "获取所有系统配置，需要登录")
    public Result<List<SystemConfigPO>> getAllConfigs() {
        List<SystemConfigPO> configs = systemConfigMapper.findAllEnabled();
        return Result.success(configs);
    }

    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/key/{key}")
    @CasbinAccess(resource = "system:config", action = "view")
    @Operation(summary = "根据键获取配置", description = "根据配置键获取单个配置")
    public Result<SystemConfigPO> getConfigByKey(@PathVariable String key) {
        SystemConfigPO config = systemConfigMapper.findByKey(key);
        if (config == null) {
            return Result.error("配置不存在: " + key);
        }
        return Result.success(config);
    }

    /**
     * 新增配置
     */
    @PostMapping
    @CasbinAccess(resource = "system:config", action = "edit")
    @Audited(module = "system", action = "CREATE", resourceType = "SystemConfig", description = "新增系统配置")
    @Operation(summary = "新增配置", description = "创建新的系统配置项")
    public Result<SystemConfigPO> createConfig(@RequestBody SystemConfigPO config) {
        if (config.getConfigKey() == null || config.getConfigKey().isEmpty()) {
            throw new IllegalArgumentException("配置键不能为空");
        }
        // Check uniqueness
        SystemConfigPO existing = systemConfigMapper.findByKey(config.getConfigKey());
        if (existing != null) {
            throw new IllegalArgumentException("配置键已存在: " + config.getConfigKey());
        }
        config.setStatus(1);
        config.setDeleted(0);
        systemConfigMapper.insert(config);
        return Result.success(config);
    }

    /**
     * 修改配置
     */
    @PutMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    @Audited(module = "system", action = "UPDATE", resourceType = "SystemConfig", description = "修改系统配置")
    @Operation(summary = "修改配置", description = "根据ID修改系统配置")
    public Result<SystemConfigPO> updateConfig(@PathVariable Long id, @RequestBody SystemConfigPO config) {
        SystemConfigPO existing = systemConfigMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        if (config.getConfigValue() != null) existing.setConfigValue(config.getConfigValue());
        if (config.getConfigLabel() != null) existing.setConfigLabel(config.getConfigLabel());
        if (config.getConfigDesc() != null) existing.setConfigDesc(config.getConfigDesc());
        if (config.getConfigType() != null) existing.setConfigType(config.getConfigType());
        if (config.getConfigGroup() != null) existing.setConfigGroup(config.getConfigGroup());
        if (config.getSortOrder() != null) existing.setSortOrder(config.getSortOrder());
        if (config.getStatus() != null) existing.setStatus(config.getStatus());
        systemConfigMapper.updateById(existing);
        return Result.success(existing);
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "system:config", action = "edit")
    @Audited(module = "system", action = "DELETE", resourceType = "SystemConfig", description = "删除系统配置")
    @Operation(summary = "删除配置", description = "根据ID删除系统配置（系统内置配置不可删除）")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        SystemConfigPO existing = systemConfigMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        if (existing.getIsSystem() != null && existing.getIsSystem() == 1) {
            throw new IllegalArgumentException("系统内置配置不可删除");
        }
        systemConfigMapper.deleteById(id);
        return Result.success(null);
    }
}
