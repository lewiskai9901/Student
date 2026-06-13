package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.stream.Stream;

/**
 * 消息域插件基类 (双轨收敛: 取代 @Deprecated MessagingDomainPlugin SPI)。
 *
 * <p>子类只声明 domainCode/domainName + triggerPoints/eventTypes/defaultTriggers 数据,
 * 本基类把它们打包成单个 {@link Contribution.EventDomainContribution} 经 contribute() 输出,
 * 由 {@link ContributionDispatcher} 调 {@link MessagingRegistrar} 写 trigger_points /
 * entity_event_types / event_triggers。industry/owns 按包路径推断 (core/education)。
 *
 * <p>与 {@link PluginPackage} 一致: 每个消息域是一个独立 PluginPackage bean
 * (参考已迁移的 DormitoryMessagingPlugin)。
 */
public abstract class AbstractMessagingPackage implements PluginPackage {

    /** 业务域码, 如 "dormitory" / "grade" / "inspection" */
    public abstract String getDomainCode();

    /** 业务域显示名 */
    public abstract String getDomainName();

    /** 触发点声明 (代码 fire 用) */
    public List<TriggerPointDef> triggerPoints() { return List.of(); }

    /** 事件类型声明 (业务语义分类) */
    public List<EventTypeDef> eventTypes() { return List.of(); }

    /** 默认触发器 (触发点 → 事件类型, admin 可覆盖) */
    public List<DefaultTriggerDef> defaultTriggers() { return List.of(); }

    // ───────── PluginManifest 元数据 (按包路径推断行业) ─────────

    @Override public String getIndustryCode() {
        return getClass().getPackageName().contains(".plugins.education") ? "EDU" : "CORE";
    }

    @Override public String getIndustryName() {
        return "EDU".equals(getIndustryCode()) ? "教育行业" : "通用核心";
    }

    @Override public List<String> getDependsOn() {
        return "EDU".equals(getIndustryCode()) ? List.of("CORE") : List.of();
    }

    @Override public boolean owns(Class<?> pluginClass) {
        String p = pluginClass.getPackageName();
        return "EDU".equals(getIndustryCode())
            ? p.contains(".plugins.education")
            : (p.contains(".plugins.core") || !p.contains(".plugins."));
    }

    @Override public final Stream<Contribution> contribute() {
        return Stream.of(new Contribution.EventDomainContribution(
            getDomainCode(), getDomainName(), triggerPoints(), eventTypes(), defaultTriggers()));
    }
}
