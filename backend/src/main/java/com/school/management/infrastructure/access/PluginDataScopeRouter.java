package com.school.management.infrastructure.access;

import com.school.management.infrastructure.extension.DataScopePlugin.DataScopeResolver;
import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件数据权限维度路由器.
 *
 * 当 role 的 data scope code 不是 core 的 5 个 enum 时(ALL/DEPARTMENT/DEPARTMENT_AND_BELOW/
 * CUSTOM/SELF), 查 data_scope_dims 表得到 resolver_type 类名, 从 Spring context 拿对应
 * bean (应实现 {@link DataScopeResolver}), 执行 resolve(userId, resourceType) 得到该用户
 * 在该维度下可访问的 resource_id 列表.
 *
 * 返回值约定:
 *   null       = 维度不存在或 resolver 不可用(调用方应安全降级 SELF)
 *   empty list = resolver 明确返回空(拒绝所有, SQL 层拼 "1=0")
 *   non-empty  = 按 id 列表过滤
 *
 * 缓存 dim_code → resolver Optional 映射, 避免频繁查表. 缓存命中: miss 也缓存(Optional.empty).
 *
 * 相关类:
 *  - {@link com.school.management.domain.access.model.DataScope#fromCodeStrict(String)}
 *  - {@link DataPermissionInterceptor}
 *  - {@link com.school.management.infrastructure.extension.DataScopePlugin}
 */
@Slf4j
@Component
public class PluginDataScopeRouter {

    private final JdbcTemplate jdbc;
    private final ApplicationContext ctx;
    private final Map<String, Optional<DataScopeResolver>> cache = new ConcurrentHashMap<>();

    public PluginDataScopeRouter(JdbcTemplate jdbc, ApplicationContext ctx) {
        this.jdbc = jdbc;
        this.ctx = ctx;
    }

    /**
     * 解析插件维度到 resource_id 列表.
     *
     * @param dimCode      维度码, 如 "BY_MAJOR"
     * @param userId       当前用户
     * @param resourceType 资源类型, 如 "student"
     * @return null=降级 SELF, empty=拒绝所有, non-empty=按 id 过滤
     */
    public List<Long> resolve(String dimCode, Long userId, String resourceType) {
        Optional<DataScopeResolver> resolverOpt = cache.computeIfAbsent(dimCode, this::loadResolver);
        if (resolverOpt.isEmpty()) {
            return null;
        }
        try {
            return resolverOpt.get().resolve(userId, resourceType);
        } catch (Exception e) {
            log.error("[PluginDataScopeRouter] resolver {} threw, degrading to SELF", dimCode, e);
            return null;
        }
    }

    /**
     * 清空缓存 — 插件 enable/disable/uninstall 或 data_scope_dims 配置变更时触发.
     * PluginPlatformController 的生命周期操作会 publish PermissionsRefreshedEvent, 此处监听清缓存.
     */
    @EventListener
    public void onPermissionsRefreshed(PermissionsRefreshedEvent event) {
        int n = cache.size();
        cache.clear();
        log.info("[PluginDataScopeRouter] cleared {} cached resolvers (source: {})", n, event.getSourceName());
    }

    /** 清空缓存 (供测试/手动触发使用). */
    public void clearCache() {
        cache.clear();
    }

    private Optional<DataScopeResolver> loadResolver(String dimCode) {
        String resolverType;
        try {
            resolverType = jdbc.queryForObject(
                "SELECT resolver_type FROM data_scope_dims WHERE dim_code = ? AND is_enabled = 1 AND plugin_enabled = 1",
                String.class, dimCode);
        } catch (Exception e) {
            log.warn("[PluginDataScopeRouter] dim {} not found in data_scope_dims: {}",
                    dimCode, e.getMessage());
            return Optional.empty();
        }
        if (resolverType == null || resolverType.isBlank()) {
            log.warn("[PluginDataScopeRouter] dim {} has no resolver_type", dimCode);
            return Optional.empty();
        }
        try {
            Class<?> clazz = Class.forName(resolverType);
            Object bean = ctx.getBean(clazz);
            if (!(bean instanceof DataScopeResolver r)) {
                log.warn("[PluginDataScopeRouter] {} is not DataScopeResolver", resolverType);
                return Optional.empty();
            }
            return Optional.of(r);
        } catch (Exception e) {
            log.warn("[PluginDataScopeRouter] failed to resolve bean for {}: {}",
                    resolverType, e.getMessage());
            return Optional.empty();
        }
    }
}
