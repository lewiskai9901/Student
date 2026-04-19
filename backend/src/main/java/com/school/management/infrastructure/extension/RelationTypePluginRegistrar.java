package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关系类型 Registrar — 扫描 {@link RelationTypePlugin}, UPSERT 到 relation_types 表.
 *
 * 继承 {@link AbstractPluginRegistrar}. 注意 industry 解析走
 * {@code packageRegistrar.resolveIndustryBySource(plugin.getSourceName())}
 * 不是默认的 resolveIndustry(Class).
 *
 * @Order(200): 在 EntityType(100) 之后, Messaging(300) 之前.
 */
@Slf4j
@Component
@Order(200)
public class RelationTypePluginRegistrar extends AbstractPluginRegistrar<RelationTypePlugin, RelationTypePlugin.RelationTypeDef> {

    private final List<RelationTypePlugin> plugins;
    private final ObjectMapper objectMapper;

    public RelationTypePluginRegistrar(List<RelationTypePlugin> plugins,
                                        JdbcTemplate jdbc,
                                        PluginPackageRegistrar packageRegistrar,
                                        ObjectMapper objectMapper) {
        super(jdbc, packageRegistrar);
        this.plugins = plugins;
        this.objectMapper = objectMapper;
    }

    @Override protected List<RelationTypePlugin> getPluginList() { return plugins; }

    @Override protected List<RelationTypePlugin.RelationTypeDef> extractDefs(RelationTypePlugin p) {
        return p.getRelationTypes();
    }

    @Override protected String describeDef(RelationTypePlugin.RelationTypeDef def) {
        return def.relationCode() + " " + def.fromType() + "→" + def.toType();
    }

    /** 关系插件的 industry 来自 sourceName 字符串 (而非类包路径) */
    @Override
    protected String resolveIndustry(RelationTypePlugin plugin) {
        return packageRegistrar.resolveIndustryBySource(plugin.getSourceName());
    }

    @Override
    protected UpsertResult upsertOne(RelationTypePlugin plugin,
                                      RelationTypePlugin.RelationTypeDef def,
                                      String industry, String pluginClass) throws Exception {
        // relation_types 复合主键 (code + from + to + tenant), CUSTOM 保护同样按 code+from+to
        if (isCustomProtected(
                "SELECT industry FROM relation_types " +
                "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
                def.relationCode(), def.fromType(), def.toType())) {
            return UpsertResult.SKIPPED;
        }
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM relation_types " +
            "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
            Long.class, def.relationCode(), def.fromType(), def.toType());

        String maxBySubtypeJson = null;
        if (def.maxBySubtype() != null && !def.maxBySubtype().isEmpty()) {
            maxBySubtypeJson = objectMapper.writeValueAsString(def.maxBySubtype());
        }

        String origin = packageRegistrar.resolveOriginBySource(plugin.getSourceName());
        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO relation_types " +
                "(relation_code, from_type, to_type, relation_name, is_transitive, category, " +
                " tier, registered_by, description, capacity_bound, max_per_resource, max_by_subtype, " +
                " industry, plugin_class, origin, is_enabled, tenant_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, NOW())",
                def.relationCode(), def.fromType(), def.toType(), def.relationName(),
                def.isTransitive() ? 1 : 0, def.category(),
                plugin.getTier(), plugin.getSourceName(), def.description(),
                def.capacityBound() ? 1 : 0, def.maxPerResource(), maxBySubtypeJson,
                industry, pluginClass, origin);
            return UpsertResult.CREATED;
        }
        jdbc.update(
            "UPDATE relation_types SET " +
            "relation_name=?, is_transitive=?, category=?, tier=?, registered_by=?, description=?, " +
            "capacity_bound=?, max_per_resource=?, max_by_subtype=?, industry=?, plugin_class=?, origin=?, is_enabled=1 " +
            "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
            def.relationName(), def.isTransitive() ? 1 : 0, def.category(),
            plugin.getTier(), plugin.getSourceName(), def.description(),
            def.capacityBound() ? 1 : 0, def.maxPerResource(), maxBySubtypeJson,
            industry, pluginClass, origin,
            def.relationCode(), def.fromType(), def.toType());
        return UpsertResult.UPDATED;
    }
}
