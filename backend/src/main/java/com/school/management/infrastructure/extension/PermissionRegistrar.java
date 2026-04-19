package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限 Registrar — 扫描所有 {@link PermissionProvider}, UPSERT 到 permissions 表.
 *
 * 继承 {@link AbstractPluginRegistrar}, 业务逻辑仅 upsertOne() 一个方法 (~30 行).
 *
 * @Order(400): 角色(500) 之前,保证角色引用的权限存在.
 */
@Slf4j
@Component
@Order(400)
public class PermissionRegistrar extends AbstractPluginRegistrar<PermissionProvider, PermissionProvider.PermissionDef> {

    private final List<PermissionProvider> providers;
    private final ApplicationEventPublisher eventPublisher;

    public PermissionRegistrar(List<PermissionProvider> providers,
                                JdbcTemplate jdbc,
                                PluginPackageRegistrar packageRegistrar,
                                ApplicationEventPublisher eventPublisher) {
        super(jdbc, packageRegistrar);
        this.providers = providers;
        this.eventPublisher = eventPublisher;
    }

    @Override protected List<PermissionProvider> getPluginList() { return providers; }

    @Override protected List<PermissionProvider.PermissionDef> extractDefs(PermissionProvider p) {
        return p.getPermissions();
    }

    @Override protected String describeDef(PermissionProvider.PermissionDef def) {
        return def.code();
    }

    /** 声明完成后发事件, Casbin / 缓存自动 reload */
    @Override
    protected void afterSync(List<PermissionProvider> plugins) {
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "PermissionRegistrar"));
    }

    @Override
    protected UpsertResult upsertOne(PermissionProvider provider,
                                      PermissionProvider.PermissionDef def,
                                      String industry, String pluginClass) {
        if (isCustomProtected(
                "SELECT industry FROM permissions WHERE permission_code=?",
                def.code())) {
            return UpsertResult.SKIPPED;
        }
        String origin = resolveOrigin(provider);
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM permissions WHERE permission_code=?",
            Long.class, def.code());
        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO permissions (permission_name, permission_code, permission_desc, " +
                "resource_type, status, industry, plugin_class, origin, created_at) " +
                "VALUES (?, ?, ?, ?, 1, ?, ?, ?, NOW())",
                def.name(), def.code(), def.description(), def.resourceType(),
                industry, pluginClass, origin);
            return UpsertResult.CREATED;
        }
        jdbc.update(
            "UPDATE permissions SET permission_name=?, permission_desc=?, " +
            "industry=?, plugin_class=?, origin=?, deleted=0, status=1 WHERE permission_code=?",
            def.name(), def.description(), industry, pluginClass, origin, def.code());
        return UpsertResult.UPDATED;
    }
}
