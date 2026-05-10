package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 统一贡献契约 — 插件通过 {@link PluginPackage#contribute()} 返回 Stream&lt;Contribution&gt;
 * 声明本包向平台贡献的所有内容.
 *
 * sealed 限定 13 个 permitted subtype, 对应旧 SPI 的 7 种职责 + 6 个扩展位
 * (route/policy/target-mode/domain/trigger-point/event-type). 每种 Contribution
 * 封装一条 def 记录, 在 ContributionDispatcher 里通过 instanceof 链分发到对应
 * Registrar 的 upsert 方法.
 *
 * Phase 2 只铺设新路径, 旧 7 SPI 的 @Component 实现仍被原 Registrar 直接扫,
 * 两条路径到同一张表 UPSERT 幂等, 不冲突.
 *
 * Track M3 (Phase 7.2 补齐): EventDomainContribution 仍保留 (整域打包语义),
 * 同时新增 TriggerPointContribution / EventTypeContribution 细粒度 permit,
 * 插件可按需选择整包或细粒度.
 */
public sealed interface Contribution permits
    Contribution.EntityTypeContribution,
    Contribution.RelationTypeContribution,
    Contribution.EventDomainContribution,
    Contribution.TriggerPointContribution,
    Contribution.EventTypeContribution,
    Contribution.PermissionContribution,
    Contribution.RoleContribution,
    Contribution.MenuContribution,
    Contribution.DataScopeContribution,
    Contribution.RouteContribution,
    Contribution.PolicyContribution,
    Contribution.TargetModeResolverContribution,
    Contribution.DomainContribution,
    Contribution.WorkflowContribution {

    /** 跨 Contribution 唯一标识, 用于冲突检测/日志 */
    String uniqueKey();

    // ═════════════════════════ 11 个 permitted 记录 ═════════════════════════

    /** 实体类型贡献 (对应旧 EntityTypePlugin 一个实例) */
    record EntityTypeContribution(EntityTypePlugin plugin) implements Contribution {
        @Override public String uniqueKey() {
            return "entity:" + plugin.getEntityType() + "/" + plugin.getTypeCode();
        }
    }

    /**
     * 关系类型贡献 (Phase 2 W2.2: 直接持有顶层 {@link RelationTypeDef}, 不再依赖旧 RelationTypePlugin SPI)
     *
     * uniqueKey 包含 (relationCode, fromType, toType): 同 relationCode 在不同
     * (subject, resource) 对下是独立关系定义 (例 admin 同时用于 (user,org_unit) 与 (user,place),
     * viewer/responsible_for 跨 user/org_unit/place 三对). PK 与 relation_types 表
     * (relation_code, from_type, to_type) 唯一索引一致.
     */
    record RelationTypeContribution(String sourceName, String tier,
                                     RelationTypeDef def) implements Contribution {
        @Override public String uniqueKey() {
            return "relation:" + def.relationCode() + ":" + def.fromType() + "/" + def.toType();
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

    /**
     * 单个触发点贡献 (Track M3) — 允许插件按需细粒度声明, 不必整 EventDomain 打包.
     *
     * domainCode / domainName 写入 trigger_points.module_code / module_name,
     * 与旧 MessagingDomainPlugin.getDomainCode()/getDomainName() 语义一致.
     *
     * DefaultTriggerDef (触发点→事件类型映射) 不进此 permit: 默认触发器偏 admin 配置,
     * 留给 DB seed 或 admin UI 管, 不用 SPI 声明. 若需要整包声明, 仍用 EventDomainContribution.
     */
    record TriggerPointContribution(String domainCode, String domainName,
                                     MessagingDomainPlugin.TriggerPointDef def)
            implements Contribution {
        @Override public String uniqueKey() { return "trigger-point:" + def.pointCode(); }
    }

    /** 单个事件类型贡献 (Track M3) — 参考 TriggerPointContribution 的细粒度语义. */
    record EventTypeContribution(String domainCode, String domainName,
                                  MessagingDomainPlugin.EventTypeDef def)
            implements Contribution {
        @Override public String uniqueKey() { return "event-type:" + def.typeCode(); }
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
     * @param industryCode 所属行业 (EDU/...), 供前端 router/plugins/loader 查找
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
     * 消息目标模式解析贡献 (Track M2) — 插件通过此 record 注入 TargetModeResolver.
     *
     * MessageDispatcher 通过 Spring DI 直接收集所有 TargetModeResolver bean 并按
     * {@link TargetModeResolver#modeCode()} 建立 Map, 本 contribution 只做登记/日志,
     * 不需 Registrar 写 DB (与 PolicyContribution 同模式).
     */
    record TargetModeResolverContribution(TargetModeResolver resolver) implements Contribution {
        @Override public String uniqueKey() { return "target-mode:" + resolver.modeCode(); }
    }

    /**
     * 领域扩展贡献 — 占位符, 留给未来非结构化贡献
     * (如跨切面的 BeanPostProcessor、事件订阅器等).
     * 当前版本不落任何 DB, 只在日志里登记.
     */
    record DomainContribution(String key, String description) implements Contribution {
        @Override public String uniqueKey() { return "domain:" + key; }
    }

    /**
     * 工作流贡献 (Phase 5) — 插件声明自带的 BPMN 文件.
     *
     * <p>启动期 {@code WorkflowContributionDeployer} 自动从 classpath 加载并 deploy
     * 到 Flowable {@code RepositoryService}.
     *
     * <p>插件 namespacing: 推荐 BPMN 内 process id 用 {@code "{industry}_{domain}_{action}"}
     * 格式 (如 {@code "edu_leave_approval"}), 避免跨插件冲突.
     *
     * @param industryCode 所属行业 (CORE/EDU/...)
     * @param resourcePath classpath 资源路径 (如 {@code "processes/leave-approval.bpmn20.xml"})
     * @param description  人类可读描述, 写入 Flowable Deployment.name
     */
    record WorkflowContribution(String industryCode, String resourcePath, String description)
            implements Contribution {
        @Override public String uniqueKey() {
            return "workflow:" + industryCode + ":" + resourcePath;
        }
    }
}
