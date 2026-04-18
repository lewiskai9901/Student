package com.school.management.infrastructure.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 关系类型插件注册器 — 启动时扫描所有 {@link RelationTypePlugin},
 * 将声明的关系类型 UPSERT 到 relation_types 表.
 *
 * 关键设计:
 *  - 关系类型由 Java 代码驱动 (不靠 SQL 迁移硬写),插件卸载 → 禁用关系
 *  - UPSERT 以 (relation_code, from_type, to_type, tenant_id) 为主键
 *  - 历史关系实例 (access_relations 表数据) 不受影响
 *
 * 执行顺序: 晚于 PluginRegistrar (类型先就位,再注册关系)
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class RelationTypePluginRegistrar implements ApplicationRunner {

    private final List<RelationTypePlugin> plugins;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (plugins.isEmpty()) {
            log.info("[RelationTypePluginRegistrar] 无插件需要注册");
            return;
        }

        int total = 0, inserted = 0, updated = 0;
        for (RelationTypePlugin plugin : plugins) {
            for (RelationTypePlugin.RelationTypeDef def : plugin.getRelationTypes()) {
                total++;
                try {
                    boolean isNew = upsert(plugin, def);
                    if (isNew) inserted++; else updated++;
                } catch (Exception e) {
                    log.error("[RelationTypePluginRegistrar] 注册失败 {} {}→{}: {}",
                        def.relationCode(), def.fromType(), def.toType(), e.getMessage());
                }
            }
        }
        log.info("[RelationTypePluginRegistrar] 扫描 {} 个插件,{} 个关系类型 (新增 {} / 更新 {})",
            plugins.size(), total, inserted, updated);
    }

    private boolean upsert(RelationTypePlugin plugin, RelationTypePlugin.RelationTypeDef def) {
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM relation_types " +
            "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
            Long.class, def.relationCode(), def.fromType(), def.toType());

        if (exists == null || exists == 0) {
            jdbc.update(
                "INSERT INTO relation_types " +
                "(relation_code, from_type, to_type, relation_name, is_transitive, category, " +
                " tier, registered_by, description, is_enabled, tenant_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, NOW())",
                def.relationCode(), def.fromType(), def.toType(), def.relationName(),
                def.isTransitive() ? 1 : 0, def.category(),
                plugin.getTier(), plugin.getSourceName(), def.description());
            return true;
        } else {
            jdbc.update(
                "UPDATE relation_types SET " +
                "relation_name=?, is_transitive=?, category=?, tier=?, registered_by=?, description=?, " +
                "is_enabled=1 " +
                "WHERE relation_code=? AND from_type=? AND to_type=? AND tenant_id=1",
                def.relationName(), def.isTransitive() ? 1 : 0, def.category(),
                plugin.getTier(), plugin.getSourceName(), def.description(),
                def.relationCode(), def.fromType(), def.toType());
            return false;
        }
    }
}
