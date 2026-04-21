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
 * <h3>当前语义 (Phase 2)</h3>
 * <ul>
 *   <li>仅做 <b>可见性</b> — 日志登记 + 跨包 uniqueKey 冲突检测</li>
 *   <li>不写 DB — 因为所有现有插件仍通过旧 @Component SPI + 原 Registrar 完成 UPSERT,
 *       此处写 DB 会造成重复</li>
 * </ul>
 *
 * <h3>未来 (Phase 3+)</h3>
 * <ul>
 *   <li>把现有插件的声明迁移到 contribute() 内</li>
 *   <li>本 Dispatcher 逐步接管 UPSERT, 旧 Registrar 逐一下线</li>
 * </ul>
 */
@Slf4j
@Component
@Order(60)
@RequiredArgsConstructor
public class ContributionDispatcher implements ApplicationRunner {

    private final List<PluginPackage> packages;

    @Override
    public void run(ApplicationArguments args) {
        if (packages == null || packages.isEmpty()) {
            log.info("[ContributionDispatcher] 无 PluginPackage 注册");
            return;
        }

        AtomicInteger total = new AtomicInteger();
        AtomicInteger entities = new AtomicInteger();
        AtomicInteger relations = new AtomicInteger();
        AtomicInteger events = new AtomicInteger();
        AtomicInteger perms = new AtomicInteger();
        AtomicInteger roles = new AtomicInteger();
        AtomicInteger menus = new AtomicInteger();
        AtomicInteger scopes = new AtomicInteger();
        AtomicInteger routes = new AtomicInteger();
        AtomicInteger policies = new AtomicInteger();
        AtomicInteger targetModes = new AtomicInteger();
        AtomicInteger domains = new AtomicInteger();

        Set<String> seenKeys = new HashSet<>();

        for (PluginPackage pkg : packages) {
            String industry = pkg.metadata().industryCode();
            pkg.contribute().forEach(c -> {
                String key = c.uniqueKey();
                if (!seenKeys.add(key)) {
                    throw new IllegalStateException(String.format(
                        "[ContributionDispatcher] 重复贡献: %s (industry=%s) — 同 uniqueKey 跨包不可重复",
                        key, industry));
                }
                total.incrementAndGet();
                // Java 17 尚未 GA pattern-switch, 用 instanceof 链 (Phase 3 升级 21 后改 switch)
                if (c instanceof Contribution.EntityTypeContribution)      entities.incrementAndGet();
                else if (c instanceof Contribution.RelationTypeContribution) relations.incrementAndGet();
                else if (c instanceof Contribution.EventDomainContribution)  events.incrementAndGet();
                else if (c instanceof Contribution.PermissionContribution)   perms.incrementAndGet();
                else if (c instanceof Contribution.RoleContribution)         roles.incrementAndGet();
                else if (c instanceof Contribution.MenuContribution)         menus.incrementAndGet();
                else if (c instanceof Contribution.DataScopeContribution)    scopes.incrementAndGet();
                else if (c instanceof Contribution.RouteContribution)        routes.incrementAndGet();
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
                log.debug("[ContributionDispatcher]   {} ← {}", key, industry);
            });
        }

        log.info("[ContributionDispatcher] 扫描 {} 个包, 收到 {} 条 Contribution " +
                "(entity {}, relation {}, event-domain {}, perm {}, role {}, menu {}, scope {}, route {}, policy {}, target-mode {}, domain {})",
            packages.size(), total.get(),
            entities.get(), relations.get(), events.get(), perms.get(),
            roles.get(), menus.get(), scopes.get(), routes.get(), policies.get(), targetModes.get(), domains.get());
    }
}
