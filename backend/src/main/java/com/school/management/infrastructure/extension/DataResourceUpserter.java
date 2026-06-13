package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据资源 Upserter — 把单条 {@link DataResourceDef} UPDATE 到 {@code data_resources.allowed_scopes}。
 *
 * <p>Phase 1 双轨收敛: 取代旧的 {@code DataResourceRegistrar} (扫描 List&lt;DataResourceProvider&gt;)。
 * 现在由 {@link ContributionDispatcher} 在分发 {@link Contribution.DataResourceContribution} 时直接调用。
 *
 * <p>语义与旧实现一致: 只 UPDATE allowed_scopes 列, 不 INSERT 新行 (行由 migration 落库);
 * 声明的 resourceCode 在表里不存在则记 WARN 并 SKIPPED, 不阻塞启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataResourceUpserter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Transactional
    public Result upsert(DataResourceDef def) {
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM data_resources WHERE resource_code=?",
            Long.class, def.resourceCode());
        if (exists == null || exists == 0) {
            log.warn("[DataResourceUpserter] resource_code={} 不存在于 data_resources, 跳过",
                    def.resourceCode());
            return Result.SKIPPED;
        }
        String allowedScopesJson;
        try {
            allowedScopesJson = objectMapper.writeValueAsString(def.allowedScopes());
        } catch (Exception e) {
            throw new IllegalStateException("allowed_scopes 序列化失败: " + def.resourceCode(), e);
        }
        jdbc.update(
            "UPDATE data_resources SET allowed_scopes=? WHERE resource_code=?",
            allowedScopesJson, def.resourceCode());
        return Result.UPDATED;
    }

    public enum Result { CREATED, UPDATED, SKIPPED }
}
