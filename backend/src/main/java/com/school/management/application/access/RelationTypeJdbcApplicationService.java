package com.school.management.application.access;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 关系字典查询的数据访问应用服务.
 *
 * <p>从 {@code RelationTypeController} 抽离 JdbcTemplate 访问.
 * camelCase 转换 / JSON 解析等表现层逻辑仍留在控制器.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RelationTypeJdbcApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_COLS =
        "SELECT relation_code, from_type, to_type, relation_name, is_transitive, " +
        "       category, tier, registered_by, description, capacity_bound, max_per_resource, max_by_subtype, " +
        "       implied_relations, industry, plugin_class, origin, is_enabled, plugin_enabled ";

    /** 公共查询 (前端业务用): 过滤被禁插件 */
    private static final String SELECT_ALL =
        SELECT_COLS + "FROM relation_types WHERE is_enabled = 1 AND plugin_enabled = 1";

    /** 管理员视角: 允许包含被禁插件贡献的关系 (灰显) */
    private static final String SELECT_ALL_FOR_ADMIN =
        SELECT_COLS + "FROM relation_types WHERE is_enabled = 1";

    /**
     * 按可选过滤条件 (tier / fromType / toType) 列出关系类型.
     * SQL 仍参数化, 用户输入不拼接进语句.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String tier, String fromType, String toType,
                                           boolean includeDisabled) {
        StringBuilder sql = new StringBuilder(
            includeDisabled ? SELECT_ALL_FOR_ADMIN : SELECT_ALL);
        List<Object> params = new ArrayList<>();
        if (tier != null && !tier.isBlank()) {
            sql.append(" AND tier = ?");
            params.add(tier);
        }
        if (fromType != null && !fromType.isBlank()) {
            sql.append(" AND from_type = ?");
            params.add(fromType);
        }
        if (toType != null && !toType.isBlank()) {
            sql.append(" AND to_type = ?");
            params.add(toType);
        }
        sql.append(" ORDER BY tier, category, relation_code");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /** 列出全部关系类型 (按 tier/category/relation_code 排序), 供分组使用. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllOrdered(boolean includeDisabled) {
        String baseSql = includeDisabled ? SELECT_ALL_FOR_ADMIN : SELECT_ALL;
        return jdbcTemplate.queryForList(
            baseSql + " ORDER BY tier, category, relation_code");
    }
}
