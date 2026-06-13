package com.school.management.infrastructure.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Phase 2 统一 SPI 的总分发器.
 *
 * 在 {@link PluginPackageRegistrar} (@Order 50) 之后、所有其他 Registrar (100+) 之前运行,
 * 遍历 {@link PluginPackage#contribute()} 流, 按 Contribution 子类型分发.
 *
 * <h3>双轨收敛收官后的语义 (2026-06-13)</h3>
 * <ul>
 *   <li><b>无依赖类型直接 UPSERT</b> (@Order 60): RelationType / DataResource / DataScope /
 *       EventDomain(messaging) / RoleScopeBinding — 调对应 Upserter/Registrar 写 DB。</li>
 *   <li><b>有依赖顺序类型由有序 Registrar 接管</b>: Permission(@Order 400) / RolePreset(500) /
 *       Menu(700) 各自过滤 contribute() 写 DB (保 permission→role→role-perm 依赖链);
 *       RolePermissionBinding 由 @Order(600) runner 写。本 Dispatcher 对这几类只计数不写, 避免双写。</li>
 *   <li><b>DI 型</b>: Policy / TargetModeResolver 只登记日志, bean 由 registry 直接收集。</li>
 *   <li>跨包 uniqueKey 冲突检测 fail-fast。</li>
 * </ul>
 * 旧声明型 @Component SPI 已全部删除, 仅 EntityTypePlugin 保留 (携带生命周期行为)。
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class ContributionDispatcher implements ApplicationRunner {

    private final List<PluginPackage> packages;
    private final MessagingRegistrar messagingRegistrar;
    private final RelationTypeUpserter relationTypeUpserter;
    private final RoleScopeBindingRegistrar roleScopeBindingRegistrar;
    private final DataResourceUpserter dataResourceUpserter;
    private final DataScopeUpserter dataScopeUpserter;
    private final PluginPackageRegistrar packageRegistrar;

    @Override
    public void run(ApplicationArguments args) {
        if (packages == null || packages.isEmpty()) {
            log.info("[ContributionDispatcher] 无 PluginPackage 注册");
            return;
        }

        AtomicInteger total = new AtomicInteger();
        AtomicInteger relations = new AtomicInteger();
        AtomicInteger events = new AtomicInteger();
        AtomicInteger triggerPoints = new AtomicInteger();
        AtomicInteger eventTypes = new AtomicInteger();
        AtomicInteger perms = new AtomicInteger();
        AtomicInteger roles = new AtomicInteger();
        AtomicInteger menus = new AtomicInteger();
        AtomicInteger scopes = new AtomicInteger();
        AtomicInteger policies = new AtomicInteger();
        AtomicInteger targetModes = new AtomicInteger();
        AtomicInteger domains = new AtomicInteger();
        AtomicInteger workflows = new AtomicInteger();
        AtomicInteger dataResources = new AtomicInteger();
        AtomicInteger roleScopes = new AtomicInteger();
        AtomicInteger rolePerms = new AtomicInteger();

        Set<String> seenKeys = new HashSet<>();

        for (PluginPackage pkg : packages) {
            String industry = pkg.metadata().industryCode();
            Class<?> pkgClass = pkg.getClass();
            pkg.contribute().forEach(c -> {
                String key = c.uniqueKey();
                if (!seenKeys.add(key)) {
                    throw new IllegalStateException(String.format(
                        "[ContributionDispatcher] 重复贡献: %s (industry=%s) — 同 uniqueKey 跨包不可重复",
                        key, industry));
                }
                total.incrementAndGet();
                // Java 17 尚未 GA pattern-switch, 用 instanceof 链 (Phase 3 升级 21 后改 switch)
                if (c instanceof Contribution.RelationTypeContribution rtc) {
                    relations.incrementAndGet();
                    try {
                        RelationTypeUpserter.Result r = relationTypeUpserter.upsert(
                            rtc.sourceName(), rtc.tier(), rtc.def());
                        log.debug("[ContributionDispatcher] {} RelationType: {} (source={}, tier={})",
                            r, rtc.def().relationCode(), rtc.sourceName(), rtc.tier());
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] 关系类型写入失败 {}: {}",
                            rtc.def().relationCode(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.EventDomainContribution edc) {
                    // 双轨收敛: 整域打包 — 写 trigger_points / entity_event_types / event_triggers。
                    // 无依赖 (event_triggers 无 FK), dispatcher @Order(60) 直接 UPSERT。
                    events.incrementAndGet();
                    try {
                        String edIndustry = packageRegistrar.resolveIndustry(pkgClass);
                        String edOrigin = packageRegistrar.resolveOrigin(pkgClass);
                        for (var tp : edc.triggerPoints())
                            messagingRegistrar.upsertTriggerPoint(edc.domainCode(), edc.domainName(), tp, pkgClass);
                        for (var et : edc.eventTypes())
                            messagingRegistrar.upsertEventType(edc.domainCode(), edc.domainName(), et, pkgClass);
                        for (var dt : edc.defaultTriggers())
                            messagingRegistrar.upsertDefaultTrigger(dt, edIndustry, pkgClass.getName(), edOrigin);
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] 消息域写入失败 {}: {}", edc.domainCode(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.TriggerPointContribution tpc) {
                    triggerPoints.incrementAndGet();
                    try {
                        messagingRegistrar.upsertTriggerPoint(
                            tpc.domainCode(), tpc.domainName(), tpc.def(), pkgClass);
                        log.info("[ContributionDispatcher] registered TriggerPoint: {} (domain={}, from={})",
                                 tpc.def().pointCode(), tpc.domainCode(), pkgClass.getSimpleName());
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] 触发点写入失败 {}: {}",
                                  tpc.def().pointCode(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.EventTypeContribution etc) {
                    eventTypes.incrementAndGet();
                    try {
                        messagingRegistrar.upsertEventType(
                            etc.domainCode(), etc.domainName(), etc.def(), pkgClass);
                        log.info("[ContributionDispatcher] registered EventType: {} (domain={}, from={})",
                                 etc.def().typeCode(), etc.domainCode(), pkgClass.getSimpleName());
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] 事件类型写入失败 {}: {}",
                                  etc.def().typeCode(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.PermissionContribution)   perms.incrementAndGet();
                else if (c instanceof Contribution.RoleContribution)         roles.incrementAndGet();
                else if (c instanceof Contribution.RoleScopeBindingContribution rsbc) {
                    roleScopes.incrementAndGet();
                    try {
                        // tenantId 从 PluginPackageRegistrar 上下文不易拿, 用默认 1
                        RoleScopeBindingRegistrar.Result r = roleScopeBindingRegistrar.upsert(
                            rsbc.roleCode(), rsbc.resourceCode(), rsbc.scopeType(), 1L);
                        log.debug("[ContributionDispatcher] RoleScopeBinding {}: {} × {} = {}",
                            r, rsbc.roleCode(), rsbc.resourceCode(), rsbc.scopeType());
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] role-scope binding 写入失败 {}: {}",
                            rsbc.uniqueKey(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.RolePermissionBindingContribution) {
                    rolePerms.incrementAndGet();
                    // UPSERT 不在这里做: dispatcher @Order(60) 跑在角色/权限注册前, 全新库会全部 skip.
                    // 真正写 role_permissions 的是 RolePermissionBindingRegistrar (@Order 600).
                }
                else if (c instanceof Contribution.MenuContribution)         menus.incrementAndGet();
                else if (c instanceof Contribution.DataScopeContribution dsc) {
                    // 无依赖类型: data_scope_dims 行注册, dispatcher @Order(60) 直接 UPSERT
                    scopes.incrementAndGet();
                    try {
                        dataScopeUpserter.upsert(dsc.domainCode(), dsc.def(),
                            packageRegistrar.resolveIndustry(pkgClass), pkgClass.getName(),
                            packageRegistrar.resolveOrigin(pkgClass));
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] 数据维度写入失败 {}: {}",
                            dsc.def().code(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.DataResourceContribution drc) {
                    // 无依赖类型: dispatcher @Order(60) 直接 UPSERT (data_resources 行由 migration 落库)
                    dataResources.incrementAndGet();
                    try {
                        dataResourceUpserter.upsert(drc.def());
                    } catch (Exception e) {
                        log.error("[ContributionDispatcher] 数据资源写入失败 {}: {}",
                            drc.def().resourceCode(), e.getMessage());
                    }
                }
                else if (c instanceof Contribution.PolicyContribution pc) {
                    policies.incrementAndGet();
                    log.info("[ContributionDispatcher] registered Policy: {} ({})",
                             pc.policy().code(), pc.policy().getClass().getSimpleName());
                }
                else if (c instanceof Contribution.TargetModeResolverContribution tmrc) {
                    targetModes.incrementAndGet();
                    log.info("[ContributionDispatcher] registered TargetModeResolver: {} ({})",
                             tmrc.resolver().modeCode(), tmrc.resolver().getClass().getSimpleName());
                    // TargetModeResolverRegistry 通过 Spring DI 直接收集 bean, 这里只登记日志 (与 Policy 同模式)
                }
                else if (c instanceof Contribution.DomainContribution)       domains.incrementAndGet();
                else if (c instanceof Contribution.WorkflowContribution) {
                    workflows.incrementAndGet();
                    // 部署由 WorkflowContributionDeployer 在 ApplicationReadyEvent 时统一做
                    // (避免 dispatcher 启动早期触发 Flowable bean 装配时序问题)
                }
                log.debug("[ContributionDispatcher]   {} ← {}", key, industry);
            });
        }

        log.info("[ContributionDispatcher] 扫描 {} 个包, 收到 {} 条 Contribution " +
                "(relation {}, event-domain {}, trigger-point {}, event-type {}, perm {}, role {}, role-scope {}, role-perm {}, menu {}, scope {}, data-resource {}, policy {}, target-mode {}, domain {}, workflow {})",
            packages.size(), total.get(),
            relations.get(), events.get(),
            triggerPoints.get(), eventTypes.get(),
            perms.get(), roles.get(), roleScopes.get(), rolePerms.get(), menus.get(), scopes.get(), dataResources.get(),
            policies.get(), targetModes.get(), domains.get(), workflows.get());
    }
}
