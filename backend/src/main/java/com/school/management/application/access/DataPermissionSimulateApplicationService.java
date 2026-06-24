package com.school.management.application.access;

import com.school.management.application.organization.MembershipResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 数据权限模拟预览的数据访问应用服务.
 *
 * <p>从 {@code DataPermissionSimulateController} 抽离, 只承载 SQL/JdbcTemplate 访问,
 * 业务计算 (scope → WHERE 构造、样本归一化) 仍留在控制器, 此处仅提供原始查询能力.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataPermissionSimulateApplicationService {

    private final JdbcTemplate jdbc;
    private final MembershipResolver membershipResolver;

    /**
     * 查询模拟用户的归属 org (DEPT / DEPT_AND_BELOW scope 需要).
     * 走统一归属入口 (access_relations member 关系), 不再读 users.primary_org_unit_id.
     * 查不到时返回 null, 不抛异常 — 与原控制器行为一致.
     */
    @Transactional(readOnly = true)
    public Long findUserOrgUnitId(Long userId) {
        return membershipResolver.orgOf(userId).orElse(null);
    }

    /**
     * 对给定表 + WHERE 子句执行 COUNT(*).
     * 注: table 与 whereClause 由调用方按白名单元数据构造, 不含用户原始输入字符串.
     */
    @Transactional(readOnly = true)
    public Long countByWhere(String table, String whereClause) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, Long.class);
    }

    /**
     * 取样本: SELECT id [, nameCol AS name] FROM table WHERE whereClause LIMIT 3.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> sampleRows(String table, String nameCol, String whereClause) {
        String nameSelect = nameCol != null ? ", " + nameCol + " AS name" : "";
        String sampleSql = "SELECT id" + nameSelect +
                " FROM " + table + " WHERE " + whereClause + " LIMIT 3";
        return jdbc.queryForList(sampleSql);
    }

    // ── 关系链预览 (P-U2): WHERE 含命名参数 → 位置参数绑定 (relation 码等字符串不内联, 防注入) ──

    /** COUNT(*) FROM table WHERE whereClause, 位置参数绑定。 */
    @Transactional(readOnly = true)
    public Long countByWhereArgs(String table, String whereClause, Object[] args) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + whereClause, Long.class, args);
    }

    /** 样本: SELECT id [, nameCol AS name] FROM table WHERE whereClause LIMIT 3, 位置参数绑定。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> sampleRowsArgs(String table, String nameCol, String whereClause, Object[] args) {
        String nameSelect = nameCol != null ? ", " + nameCol + " AS name" : "";
        return jdbc.queryForList(
            "SELECT id" + nameSelect + " FROM " + table + " WHERE " + whereClause + " LIMIT 3", args);
    }

    /** 漏斗每跳计数: COUNT(*) FROM (子查询) t, 位置参数绑定。子查询由 ChainHopResolver 产出。 */
    @Transactional(readOnly = true)
    public Long countSubquery(String subquerySql, Object[] args) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM (" + subquerySql + ") _funnel", Long.class, args);
    }
}
