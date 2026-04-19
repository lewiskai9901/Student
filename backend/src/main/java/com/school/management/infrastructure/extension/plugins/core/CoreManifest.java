package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.PluginPackage;
import org.springframework.stereotype.Component;

/**
 * 通用核心插件包 — 任何部署都需要,不可卸载.
 *
 * 包含:
 *  - 9 个核心关系类型 (CoreRelationsPlugin)
 *  - 通用权限 / 超级管理员角色
 *  - 通用事件类型(USER_CREATED / ORG_MERGED 等)
 *
 * Phase 2: 升级为 PluginPackage (继承自 PluginManifest, 默认 contribute()=空流).
 * 现有所有贡献仍通过旧 @Component SPI 路径扫描, 行为不变.
 */
@Component
public class CoreManifest implements PluginPackage {

    @Override
    public String getIndustryCode() { return "CORE"; }

    @Override
    public String getIndustryName() { return "通用核心"; }

    @Override
    public boolean owns(Class<?> pluginClass) {
        String pkg = pluginClass.getPackageName();
        // core 包 + 未标注 industry 的旧插件(保守落到 CORE)
        return pkg.contains(".plugins.core") || !pkg.contains(".plugins.");
    }
}
