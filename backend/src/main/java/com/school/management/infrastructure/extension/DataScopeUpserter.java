package com.school.management.infrastructure.extension;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据维度 Upserter — 把单条 {@link DataScopeDimensionDef} UPSERT 到 {@code data_scope_dims}。
 *
 * <p>Phase 2 双轨收敛: 取代旧的 {@code DataScopeRegistrar} (扫描 List&lt;DataScopePlugin&gt;)。
 * 现在由 {@link ContributionDispatcher} 分发 {@link Contribution.DataScopeContribution} 时调用。
 * 语义与旧实现一致: 复合 key=dim_code; CUSTOM(管理员自创)不被插件覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataScopeUpserter {

    private final JdbcTemplate jdbc;

    @Transactional
    public Result upsert(String domainCode, DataScopeDimensionDef def,
                         String industry, String pluginClass, String origin) {
        String existingIndustry = jdbc.query(
            "SELECT industry FROM data_scope_dims WHERE dim_code=?",
            rs -> rs.next() ? rs.getString(1) : null, def.code());
        if ("CUSTOM".equals(existingIndustry)) {
            return Result.SKIPPED;
        }
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM data_scope_dims WHERE dim_code=?",
            Long.class, def.code());
        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO data_scope_dims (dim_code, dim_name, description, domain_code, " +
                "resolver_type, industry, plugin_class, origin, is_enabled, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, NOW())",
                def.code(), def.name(), def.description(), domainCode,
                def.resolverType(), industry, pluginClass, origin);
            return Result.CREATED;
        }
        jdbc.update(
            "UPDATE data_scope_dims SET dim_name=?, description=?, domain_code=?, " +
            "resolver_type=?, industry=?, plugin_class=?, origin=?, is_enabled=1 " +
            "WHERE dim_code=?",
            def.name(), def.description(), domainCode,
            def.resolverType(), industry, pluginClass, origin, def.code());
        return Result.UPDATED;
    }

    public enum Result { CREATED, UPDATED, SKIPPED }
}
