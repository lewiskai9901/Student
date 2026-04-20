package com.school.management.infrastructure.extension;

import java.util.stream.Stream;

/**
 * 统一插件包 SPI — Phase 2 新增的顶层协议.
 *
 * 继承自 {@link PluginManifest} 复用 metadata 字段 (industryCode/version/dependsOn...),
 * 并新增 {@link #contribute()} 返回 {@link Contribution} 流, 把旧 7 SPI 的职责
 * (实体类型/关系/消息域/权限/角色/菜单/数据范围) 统一到一个声明点.
 *
 * <h3>与旧 SPI 的关系 (Phase 2 过渡策略)</h3>
 *
 * <ul>
 *   <li>旧 7 SPI (EntityTypePlugin 等) 已打 {@code @Deprecated}, 其 {@code @Component} 实现仍被
 *       对应 Registrar 扫描, 原有行为不变.</li>
 *   <li>新插件可完全走 PluginPackage, 不再写一堆 {@code @Component} 小类.</li>
 *   <li>两条路径写同一张表, UPSERT 幂等, 混用无副作用.</li>
 * </ul>
 *
 * <h3>迁移示例 (后续会话做, Phase 2 只铺底座)</h3>
 * <pre>
 * &#64;Component
 * public class EducationManifest implements PluginPackage {
 *     // 继承 PluginManifest 的 getIndustryCode() 等 (已实现)
 *     &#64;Override
 *     public Stream&lt;Contribution&gt; contribute() {
 *         return Stream.of(
 *             new Contribution.EntityTypeContribution(studentPlugin),
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
     * (如 HEALTH 的"默认病区前缀"、"自动床位分配"开关), 覆盖此方法.
     *
     * 读配置走 {@code TenantPluginService.getConfig()}.
     */
    default PluginConfigSchema configSchema() {
        return PluginConfigSchema.empty();
    }

    /**
     * Phase 7.4 skeleton: 声明本插件期望的 DB schema 版本.
     *
     * 默认 0 — 无 schema. 插件声明自己 schema 版本后, 启动时可以:
     * - 检查对应 database/plugins/{industryCode}/V*.sql migrations 是否执行
     * - 禁用插件前写入 "disabled" 标记, 后续启用时跳过冲突 migrations
     *
     * 当前仅 SPI 骨架, Flyway 实际布线留 Phase 8+.
     */
    default int schemaVersion() {
        return 0;
    }

    /**
     * Phase 7.4 skeleton: 插件 schema migrations 所在 classpath 位置.
     *
     * 约定: {@code classpath:db/migration/plugins/{industryCode}/}.
     * 返回 null 表示此插件无独立 migrations.
     */
    default String schemaMigrationLocation() {
        return null;
    }
}
