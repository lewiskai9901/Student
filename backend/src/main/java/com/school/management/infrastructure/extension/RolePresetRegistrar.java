package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 预置角色 Registrar — 扫描 {@link RolePresetPlugin}, UPSERT 到 roles 表.
 *
 * 继承 {@link AbstractPluginRegistrar}, 业务仅 upsertOne() 一个方法.
 *
 * 策略:
 *  - 匹配 role_code 唯一键(含软删)
 *  - 已存在(含软删)→ UPDATE 恢复 (deleted=0, status=1)
 *  - CUSTOM 保护: admin 自创角色不被插件覆盖
 *  - 不覆盖 role_desc(可能被 admin 改过)
 */
@Slf4j
@Component
@Order(500)
public class RolePresetRegistrar extends AbstractPluginRegistrar<RolePresetPlugin, RolePresetPlugin.RolePresetDef> {

    private final List<RolePresetPlugin> plugins;
    private final ApplicationEventPublisher eventPublisher;

    public RolePresetRegistrar(List<RolePresetPlugin> plugins,
                                JdbcTemplate jdbc,
                                PluginPackageRegistrar packageRegistrar,
                                ApplicationEventPublisher eventPublisher) {
        super(jdbc, packageRegistrar);
        this.plugins = plugins;
        this.eventPublisher = eventPublisher;
    }

    @Override protected List<RolePresetPlugin> getPluginList() { return plugins; }

    @Override protected List<RolePresetPlugin.RolePresetDef> extractDefs(RolePresetPlugin p) {
        return p.getPresets();
    }

    @Override protected String describeDef(RolePresetPlugin.RolePresetDef def) {
        return def.roleCode();
    }

    /** 声明完成后发事件, Casbin 自动 reload */
    @Override
    protected void afterSync(List<RolePresetPlugin> plugins) {
        eventPublisher.publishEvent(new PermissionsRefreshedEvent(this, "RolePresetRegistrar"));
    }

    @Override
    protected UpsertResult upsertOne(RolePresetPlugin plugin,
                                      RolePresetPlugin.RolePresetDef def,
                                      String industry, String pluginClass) {
        if (isCustomProtected(
                "SELECT industry FROM roles WHERE role_code=?",
                def.roleCode())) {
            return UpsertResult.SKIPPED;
        }
        String origin = resolveOrigin(plugin);
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM roles WHERE role_code=?",
            Long.class, def.roleCode());

        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO roles (role_code, role_name, role_desc, role_type, status, " +
                "industry, plugin_class, origin, created_at) " +
                "VALUES (?, ?, ?, ?, 1, ?, ?, ?, NOW())",
                def.roleCode(), def.roleName(), def.description(), def.roleType(),
                industry, pluginClass, origin);
            return UpsertResult.CREATED;
        }
        jdbc.update(
            "UPDATE roles SET role_name=?, role_type=?, industry=?, plugin_class=?, origin=?, " +
            "deleted=0, status=1 WHERE role_code=?",
            def.roleName(), def.roleType(), industry, pluginClass, origin, def.roleCode());
        return UpsertResult.UPDATED;
    }
}
