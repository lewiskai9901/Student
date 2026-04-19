package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 统一贡献契约 — 插件通过 {@link PluginPackage#contribute()} 返回 Stream&lt;Contribution&gt;
 * 声明本包向平台贡献的所有内容.
 *
 * sealed 限定 8 个 permitted subtype, 对应旧 SPI 的 7 种职责 + 1 个扩展位.
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
    Contribution.DomainContribution {

    /** 跨 Contribution 唯一标识, 用于冲突检测/日志 */
    String uniqueKey();

    // ═════════════════════════ 8 个 permitted 记录 ═════════════════════════

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
     * 领域扩展贡献 — 占位符, 留给未来非结构化贡献
     * (如跨切面的 BeanPostProcessor、事件订阅器等).
     * 当前版本不落任何 DB, 只在日志里登记.
     */
    record DomainContribution(String key, String description) implements Contribution {
        @Override public String uniqueKey() { return "domain:" + key; }
    }
}
