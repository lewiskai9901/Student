package com.school.management.infrastructure.access;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 启动期校验:每个 PROVIDER 关系的 {@code resolver_bean} 都能解析到真实
 * {@link com.school.management.infrastructure.extension.RecordRelationResolver} bean (R3c 守护 / P4-E)。
 *
 * <p>为何需要:resolver bean 名拼错 / 插件未装配时,运行期是<b>静默 fail-closed 拒绝所有</b>
 * (安全但难排查 —— 用户只见"查不到数据")。本校验把它<b>提前到启动期 loud 暴露</b>:
 * 列出所有缺失的 (resource/relation → bean)。不 fail-fast (一个坏插件不该拖垮整库;运行期 fail-closed
 * 仍兜底安全),但 WARN 足够醒目。
 *
 * <p>在 {@link ResourceRelationRegistry#load} (同 ApplicationReadyEvent) 之后跑 —— 用较大 @Order 保证。
 */
@Slf4j
@Component
@Order(1000)
public class ProviderResolverStartupValidator {

    private final ResourceRelationRegistry registry;
    private final RecordRelationResolverRouter router;

    public ProviderResolverStartupValidator(ResourceRelationRegistry registry,
                                            RecordRelationResolverRouter router) {
        this.registry = registry;
        this.router = router;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        Map<String, String> providers = registry.providerResolverBeans();
        if (providers.isEmpty()) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> e : providers.entrySet()) {
            String bean = e.getValue();
            if (bean == null || bean.isBlank() || router.resolve(bean).isEmpty()) {
                missing.add(e.getKey() + " → '" + bean + "'");
            }
        }
        if (missing.isEmpty()) {
            log.info("[ProviderResolverStartupValidator] {} 个 PROVIDER 关系的 resolver bean 全部就位", providers.size());
        } else {
            log.warn("[ProviderResolverStartupValidator] {}/{} 个 PROVIDER 关系 resolver bean 缺失 → 运行期将 fail-closed 拒绝! "
                    + "请检查插件装配 / bean 名拼写: {}", missing.size(), providers.size(), missing);
        }
    }
}
