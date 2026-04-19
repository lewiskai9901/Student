package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 插件包元数据的不可变视图 — 由 {@link PluginPackage#metadata()} 返回.
 *
 * 合并了 {@link PluginManifest} 的 6 个 getter 到一个 record,
 * 供 ContributionDispatcher / 治理 API / 诊断日志使用.
 * 老代码继续用 PluginManifest getter, 新代码用 metadata() 更简洁.
 */
public record PluginMetadata(
    String industryCode,
    String industryName,
    String version,
    List<String> dependsOn,
    Map<String, String> dependsOnWithVersion,
    PluginManifest.UninstallPolicy uninstallPolicy
) {
    /** 从现有 PluginManifest 桥接 (默认实现 PluginPackage.metadata() 用) */
    public static PluginMetadata of(PluginManifest m) {
        return new PluginMetadata(
            m.getIndustryCode(),
            m.getIndustryName(),
            m.getVersion(),
            m.getDependsOn(),
            m.getDependsOnWithVersion(),
            m.getUninstallPolicy()
        );
    }
}
