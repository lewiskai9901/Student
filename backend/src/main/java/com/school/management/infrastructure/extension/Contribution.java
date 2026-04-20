package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 统一贡献契约 — 插件通过 {@link PluginPackage#contribute()} 返回 Stream&lt;Contribution&gt;
 * 声明本包向平台贡献的所有内容.
 *
 * sealed 限定 10 个 permitted subtype, 对应旧 SPI 的 7 种职责 + 3 个扩展位 (route/policy/domain).
 * 每种 Contribution 封装一条 def 记录, 在 ContributionDispatcher 里
 * 通过 pattern-matching switch 分发到对应 Registrar 的 upsert 方法.
 *
 * Phase 2 只铺设新路径, 旧 7 SPI 的 @Component 实现仍被原 Registrar 直接扫,
 * 两条路径到同一张表 UPSERT 幂等, 不冲突.
 */
public sealed interface Contribution permits
    Contribution.EntityTypeContribution,
    Contribution.RelationTypeContribution,
    Contribution.EventDomainContribution,
    Contribution.PermissionContribution,
    Contribution.RoleContribution,
    Contribution.MenuContribution,
    Contribution.DataScopeContribution,
    Contribution.RouteContribution,
    Contribution.PolicyContribution,
    Contribution.DomainContribution {

    /** 跨 Contribution 唯一标识, 用于冲突检测/日志 */
    String uniqueKey();

    // ═════════════════════════ 10 个 permitted 记录 ═════════════════════════

    /** 实体类型贡献 (对应旧 EntityTypePlugin 一个实例) */
    record EntityTypeContribution(EntityTypePlugin plugin) implements Contribution {
        @Override public String uniqueKey() {
            return "entity:" + plugin.getEntityType() + "/" + plugin.getTypeCode();
        }
    }

    /** 关系类型贡献 (对应旧 RelationTypePlugin 单条声明) */
    record RelationTypeContribution(String sourceName, String tier,
                                     RelationTypePlugin.RelationTypeDef def) implements Contribution {
        @Override public String uniqueKey() {
            return "relation:" + def.relationCode();
        }
    }

    /** 消息域贡献 — 一个域打包: 触发点 + 事件类型 + 默认触发器 */
    record EventDomainContribution(String domainCode, String domainName,
                                    List<MessagingDomainPlugin.TriggerPointDef> triggerPoints,
                                    List<MessagingDomainPlugin.EventTypeDef> eventTypes,
                                    List<MessagingDomainPlugin.DefaultTriggerDef> defaultTriggers)
            implements Contribution {
        @Override public String uniqueKey() { return "event-domain:" + domainCode; }
    }

    /** 权限贡献 (对应旧 PermissionDef) */
    record PermissionContribution(String moduleCode, String moduleName,
                                   PermissionProvider.PermissionDef def) implements Contribution {
        @Override public String uniqueKey() { return "perm:" + def.code(); }
    }

    /** 预置角色贡献 */
    record RoleContribution(RolePresetPlugin.RolePresetDef def) implements Contribution {
        @Override public String uniqueKey() { return "role:" + def.roleCode(); }
    }

    /** 菜单贡献 */
    record MenuContribution(String domainCode,
                             MenuContributionPlugin.MenuItemDef item) implements Contribution {
        @Override public String uniqueKey() { return "menu:" + item.path(); }
    }

    /** 数据范围贡献 */
    record DataScopeContribution(String domainCode,
                                  DataScopePlugin.DimensionDef def) implements Contribution {
        @Override public String uniqueKey() { return "data-scope:" + def.code(); }
    }

    /**
     * 前端路由贡献 (Phase 7.1)
     *
     * 声明插件向前端提供的 SPA 路由元数据. 前端 bootstrap 拉 overview 时
     * 会读这批元数据, 和 frontend/src/router/plugins/{code}.ts 里的静态 routes
     * 互补: SPI 用于 admin 审计/UI 可视化, 静态 routes 用于懒加载组件.
     *
     * 当前 ContributionDispatcher 只登记不下发到前端. Phase 8 可加 endpoint
     * /api/plugin-platform/routes 让前端动态构造.
     *
     * @param industryCode 所属行业 (EDU/HEALTH/...), 供前端 router/plugins/loader 查找
     * @param routePath    顶级路由路径 (如 /patient, /student), 与前端一致
     * @param title        导航显示名
     * @param requiresAuth 是否需要登录 (默认 true)
     */
    record RouteContribution(String industryCode, String routePath, String title,
                              boolean requiresAuth) implements Contribution {
        @Override public String uniqueKey() { return "route:" + industryCode + ":" + routePath; }

        public static RouteContribution of(String industry, String path, String title) {
            return new RouteContribution(industry, path, title, true);
        }
    }

    /**
     * 策略贡献 (Track W1) — 插件通过此 record 注入 Policy 实现.
     *
     * PolicyRegistry 通过 Spring DI 直接收集所有 Policy bean, 本 contribution
     * 只做登记/日志, 不需 Registrar 写 DB.
     */
    record PolicyContribution(Policy<?> policy) implements Contribution {
        @Override public String uniqueKey() { return "policy:" + policy.code(); }
    }

    /**
     * 领域扩展贡献 — 占位符, 留给未来非结构化贡献
     * (如跨切面的 BeanPostProcessor、事件订阅器等).
     * 当前版本不落任何 DB, 只在日志里登记.
     */
    record DomainContribution(String key, String description) implements Contribution {
        @Override public String uniqueKey() { return "domain:" + key; }
    }
}
