package com.school.management.infrastructure.extension;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据权限维度 Registrar — 扫描 {@link DataScopePlugin}, UPSERT 到 data_scope_dims 表.
 *
 * 建表由 V20260420_1__plugin_infrastructure_foundation.sql 保证 (不再运行时 CREATE).
 *
 * @Order(600): 在 Permission / RolePreset 之后.
 */
@Slf4j
@Component
@Order(600)
public class DataScopeRegistrar extends AbstractPluginRegistrar<DataScopePlugin, DataScopePlugin.DimensionDef> {

    private final List<DataScopePlugin> plugins;

    public DataScopeRegistrar(List<DataScopePlugin> plugins,
                               JdbcTemplate jdbc,
                               PluginPackageRegistrar packageRegistrar) {
        super(jdbc, packageRegistrar);
        this.plugins = plugins;
    }

    @Override protected List<DataScopePlugin> getPluginList() { return plugins; }

    @Override protected List<DataScopePlugin.DimensionDef> extractDefs(DataScopePlugin p) {
        return p.getDimensions();
    }

    @Override protected String describeDef(DataScopePlugin.DimensionDef def) { return def.code(); }

    @Override
    protected UpsertResult upsertOne(DataScopePlugin plugin,
                                      DataScopePlugin.DimensionDef def,
                                      String industry, String pluginClass) {
        if (isCustomProtected(
                "SELECT industry FROM data_scope_dims WHERE dim_code=?",
                def.code())) {
            return UpsertResult.SKIPPED;
        }
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM data_scope_dims WHERE dim_code=?",
            Long.class, def.code());

        String origin = resolveOrigin(plugin);
        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO data_scope_dims (dim_code, dim_name, description, domain_code, " +
                "resolver_type, industry, plugin_class, origin, is_enabled, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, NOW())",
                def.code(), def.name(), def.description(), plugin.getDomainCode(),
                def.resolverType(), industry, pluginClass, origin);
            return UpsertResult.CREATED;
        }
        jdbc.update(
            "UPDATE data_scope_dims SET dim_name=?, description=?, domain_code=?, " +
            "resolver_type=?, industry=?, plugin_class=?, origin=?, is_enabled=1 " +
            "WHERE dim_code=?",
            def.name(), def.description(), plugin.getDomainCode(),
            def.resolverType(), industry, pluginClass, origin, def.code());
        return UpsertResult.UPDATED;
    }
}
