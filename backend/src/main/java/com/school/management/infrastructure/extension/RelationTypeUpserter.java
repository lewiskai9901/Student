package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 关系类型 Upserter — 把单条 {@link RelationTypeDef} UPSERT 到 {@code relation_types} 表.
 *
 * <p>Phase 2 W2.2: 取代旧的 {@code RelationTypePluginRegistrar} (扫描 List&lt;RelationTypePlugin&gt;).
 * 现在由 {@link ContributionDispatcher} 在分发 {@link Contribution.RelationTypeContribution}
 * 时直接调用 {@link #upsert(String, String, RelationTypeDef)}.
 *
 * <p>关键约束:
 * <ul>
 *   <li>industry 解析: 通过 {@code PluginPackageRegistrar.resolveIndustryBySource(sourceName)} —
 *       与旧实现兼容 ("CORE" / "EducationPlugin" → CORE/EDU).</li>
 *   <li>CUSTOM 保护: 已被管理员标记 industry=CUSTOM 的记录不被插件覆盖.</li>
 *   <li>UPSERT 语义: 复合 key = (relation_code, from_type, to_type, tenant_id=1).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelationTypeUpserter {

    private final JdbcTemplate jdbc;
    private final PluginPackageRegistrar packageRegistrar;
    private final ObjectMapper objectMapper;

    /**
     * UPSERT 一条关系定义.
     *
     * @param sourceName  registered_by (e.g. "CORE", "EducationPlugin")
     * @param tier        关系层级 (CORE / DOMAIN / COMMON_EXT)
     * @param def         关系定义
     * @return CREATED / UPDATED / SKIPPED (CUSTOM 保护)
     */
    @Transactional
    public Result upsert(String sourceName, String tier, RelationTypeDef def) {
        try {
            return doUpsert(sourceName, tier, def);
        } catch (Exception e) {
            log.error("[RelationTypeUpserter] 写入失败 {} {}→{}: {}",
                def.relationCode(), def.fromType(), def.toType(), e.getMessage());
            throw new RuntimeException("RelationTypeUpserter 失败: " + e.getMessage(), e);
        }
    }

    private Result doUpsert(String sourceName, String tier, RelationTypeDef def) throws Exception {
        // CUSTOM 保护
        String existingIndustry = jdbc.query(
            "SELECT industry FROM relation_types " +
            "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
            rs -> rs.next() ? rs.getString(1) : null,
            def.relationCode(), def.fromType(), def.toType());
        if ("CUSTOM".equals(existingIndustry)) {
            return Result.SKIPPED;
        }

        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM relation_types " +
            "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
            Long.class, def.relationCode(), def.fromType(), def.toType());

        String maxBySubtypeJson = null;
        if (def.maxBySubtype() != null && !def.maxBySubtype().isEmpty()) {
            maxBySubtypeJson = objectMapper.writeValueAsString(def.maxBySubtype());
        }

        String impliedJson = null;
        if (def.impliedRelations() != null && !def.impliedRelations().isEmpty()) {
            impliedJson = objectMapper.writeValueAsString(def.impliedRelations());
        }

        String industry = packageRegistrar.resolveIndustryBySource(sourceName);
        String origin = packageRegistrar.resolveOriginBySource(sourceName);
        // pluginClass 字段保留兼容: 没有具体插件类时填 manifest 来源标识
        String pluginClass = "PluginPackage:" + (industry != null ? industry : sourceName);

        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO relation_types " +
                "(relation_code, from_type, to_type, relation_name, is_transitive, category, " +
                " tier, registered_by, description, capacity_bound, max_per_resource, max_by_subtype, " +
                " implied_relations, industry, plugin_class, origin, is_enabled, tenant_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, NOW())",
                def.relationCode(), def.fromType(), def.toType(), def.relationName(),
                def.isTransitive() ? 1 : 0, def.category(),
                tier, sourceName, def.description(),
                def.capacityBound() ? 1 : 0, def.maxPerResource(), maxBySubtypeJson,
                impliedJson, industry, pluginClass, origin);
            return Result.CREATED;
        }

        jdbc.update(
            "UPDATE relation_types SET " +
            "relation_name=?, is_transitive=?, category=?, tier=?, registered_by=?, description=?, " +
            "capacity_bound=?, max_per_resource=?, max_by_subtype=?, implied_relations=?, " +
            "industry=?, plugin_class=?, origin=?, is_enabled=1 " +
            "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
            def.relationName(), def.isTransitive() ? 1 : 0, def.category(),
            tier, sourceName, def.description(),
            def.capacityBound() ? 1 : 0, def.maxPerResource(), maxBySubtypeJson, impliedJson,
            industry, pluginClass, origin,
            def.relationCode(), def.fromType(), def.toType());
        return Result.UPDATED;
    }

    public enum Result { CREATED, UPDATED, SKIPPED }
}
