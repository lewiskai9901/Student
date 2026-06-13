package com.school.management.infrastructure.extension;

import java.util.stream.Stream;

/**
 * 统一插件包 SPI — Phase 2 新增的顶层协议.
 *
 * 继承自 {@link PluginManifest} 复用 metadata 字段 (industryCode/version/dependsOn...),
 * 并新增 {@link #contribute()} 返回 {@link Contribution} 流, 把声明型贡献
 * (关系/消息域/权限/角色/菜单/数据范围/数据资源) 统一到一个声明点.
 *
 * <h3>与扩展 SPI 的关系 (双轨收敛收官 2026-06-13)</h3>
 *
 * <ul>
 *   <li>6 个声明型 @Component SPI 已全部删除并迁到 {@link #contribute()}, 这里是唯一声明点.</li>
 *   <li>唯一保留的旧 SPI 是 {@link EntityTypePlugin} — 它携带生命周期行为 (beforeCreate/
 *       afterCreate/validate), 与 {@link Policy} / {@link TargetModeResolver} 同属
 *       "携带行为的 bean SPI", 由 Spring 扫描 {@code @Component} 实现直接注册, 不经 contribute().</li>
 * </ul>
 *
 * <h3>声明示例</h3>
 * <pre>
 * &#64;Component
 * public class EducationManifest implements PluginPackage {
 *     // 继承 PluginManifest 的 getIndustryCode() 等 (已实现)
 *     &#64;Override
 *     public Stream&lt;Contribution&gt; contribute() {
 *         return Stream.of(
 *             new Contribution.RoleContribution(teacherRoleDef),
 *             new Contribution.PermissionContribution("teaching", "教学", permDef)
 *         );
 *     }
 * }
 * </pre>
 */
public interface PluginPackage extends PluginManifest {

    /** 元数据视图, 默认从 PluginManifest getter 桥接 */
    default PluginMetadata metadata() {
        return PluginMetadata.of(this);
    }

    /** 本包贡献的所有 Contribution (默认空, 旧 Manifest 不受影响) */
    default Stream<Contribution> contribute() {
        return Stream.empty();
    }

    /**
     * Phase 7.5: 声明本插件接受的运行时配置项.
     *
     * 默认返回空 schema — 插件不接受任何配置. 若需要让管理员配置
     * (如 EDU 的"默认年级前缀"、"自动班级分配"开关), 覆盖此方法.
     *
     * 读配置走 {@code TenantPluginService.getConfig()}.
     */
    default PluginConfigSchema configSchema() {
        return PluginConfigSchema.empty();
    }
}
