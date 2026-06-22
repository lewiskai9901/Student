package com.school.management.infrastructure.access;

import com.school.management.infrastructure.extension.RecordRelationResolver;
import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PROVIDER 关系的 resolver bean 查找 (R3c)。按 {@code resource_relations.resolver_bean} 的 bean 名
 * 从 Spring 容器取 {@link RecordRelationResolver},缓存结果。镜像 {@link PluginDataScopeRouter} 的形态。
 *
 * <p><b>fail-closed</b>:bean 不存在 / 类型不符 → 返回 {@link Optional#empty()},引擎据此注入
 * {@code 1=0} 拒绝所有(决策点:安全优先于可用性)。
 */
@Slf4j
@Component
public class RecordRelationResolverRouter {

    private final ApplicationContext ctx;
    private final Map<String, Optional<RecordRelationResolver>> cache = new ConcurrentHashMap<>();

    public RecordRelationResolverRouter(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /** 按 bean 名取 resolver;不存在/不可用 → empty (fail-closed)。 */
    public Optional<RecordRelationResolver> resolve(String beanName) {
        if (beanName == null || beanName.isBlank()) {
            return Optional.empty();
        }
        return cache.computeIfAbsent(beanName, this::load);
    }

    private Optional<RecordRelationResolver> load(String beanName) {
        try {
            return Optional.of(ctx.getBean(beanName, RecordRelationResolver.class));
        } catch (BeansException e) {
            log.error("[RecordRelationResolverRouter] PROVIDER resolver bean '{}' 不可用 → fail-closed 拒绝所有", beanName, e);
            return Optional.empty();
        }
    }

    /** 插件启停 / 权限刷新后清缓存 (bean 可能新增/移除)。 */
    @EventListener(PermissionsRefreshedEvent.class)
    public void onRefresh(PermissionsRefreshedEvent event) {
        int n = cache.size();
        cache.clear();
        log.info("[RecordRelationResolverRouter] cleared {} cached resolvers (source: {})", n, event.getSourceName());
    }
}
