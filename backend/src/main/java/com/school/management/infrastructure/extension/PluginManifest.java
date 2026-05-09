package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 插件包元信息 SPI.
 *
 * 每个行业(如 CORE / EDU)提供一个 {@code @Component} Manifest 实现,
 * 启动时由 {@code PluginPackageRegistrar} 扫描并落 {@code plugin_packages} 表.
 *
 * Manifest 负责:
 *  - 声明行业码、版本、依赖关系(供启动顺序拓扑排序)
 *  - 声明卸载策略
 *  - 提供行业标签,供其他 Registrar 在 UPSERT 时给记录打 industry 列
 */
public interface PluginManifest {

    /** 行业码,作为 plugin_packages 主键,如 "CORE" / "EDU" */
    String getIndustryCode();

    /** 行业显示名 */
    String getIndustryName();

    /** 版本号 (SemVer) */
    default String getVersion() { return "1.0.0"; }

    /** 依赖的其他行业 industry_code,本行业启动前这些必须已 enabled */
    default List<String> getDependsOn() { return List.of(); }

    /**
     * 带版本范围的依赖声明 (SemVer Phase 8).
     *
     * 返回 Map: industry_code -> versionRange.
     * versionRange 支持三种简单形式:
     *   ">=1.0.0"      — 至少 1.0.0
     *   ">=1.0.0 <2.0.0" — 兼容 1.x
     *   "*"            — 任意版本 (等价于不指定)
     *
     * 默认返回空 Map → 退化到 getDependsOn() (无版本约束).
     * 启动时由 PluginPackageRegistrar 校验, 不兼容 fail-fast.
     */
    default java.util.Map<String, String> getDependsOnWithVersion() {
        return java.util.Map.of();
    }

    /** 卸载策略 */
    default UninstallPolicy getUninstallPolicy() { return UninstallPolicy.SOFT; }

    /**
     * 判断一个 plugin 类是否属于本行业.
     * 默认按包路径约定: plugins/{industry}/... 对应 industry_code={INDUSTRY}.
     */
    default boolean owns(Class<?> pluginClass) {
        String pkg = pluginClass.getPackageName().toLowerCase();
        String token = "." + getIndustryCode().toLowerCase() + ".";
        return pkg.contains(token);
    }

    enum UninstallPolicy {
        /** 禁用类型/关系/事件,保留业务实例数据 */
        SOFT,
        /** 物理删除所有插件数据(仅开发环境) */
        HARD,
        /** 冻结新建,所有老数据可读 */
        FROZEN
    }
}
