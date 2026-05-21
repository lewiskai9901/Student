package com.school.management.infrastructure.tenant;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Properties;
import java.util.Set;

/**
 * MyBatis interceptor 占位 — 自动注入 tenant_id 过滤(未实装).
 *
 * <p>━━━━━ 决策记录 (2026-05-21) ━━━━━
 * <p><b>本项目按单租户运行,多租户功能整体暂缓 (deferred,非废弃)。</b>
 * 经评估,彻底拆除多租户脚手架 (314 个 DB tenant_id 列 + ~210 个文件 +
 * 数据权限拦截器重写) 是数天、极高风险且功能收益为零的改动 —— 故决定
 * <b>保持现有多租户脚手架休眠</b>:
 * <ul>
 *   <li>全系统按 {@code tenant=1} 运行 ({@code TenantContextHolder} 默认值)</li>
 *   <li>各表 {@code tenant_id} 列保留,值恒为 1,无害</li>
 *   <li>本拦截器 {@code @Component} 故意注释掉 —— 不注册,不生效</li>
 * </ul>
 * <p>将来若要真正启用多租户:实装本类的 SQL 注入逻辑 (或改用 MyBatis-Plus
 * {@code TenantLineInnerInterceptor}) 并恢复 {@code @Component};JdbcTemplate
 * 直查路径与 {@code @Async} 上下文传递需另行补齐。详见 docs/design 下的 ADR。
 * <p>━━━━━━━━━━━━━━━━━━━━━━━━━━━
 *
 * <p>原始设计:For SELECT/UPDATE/DELETE 追加 {@code WHERE tenant_id = ?};
 * INSERT 由实体默认值处理 (tenant_id 列默认 = 1).
 */
@Slf4j
// @Component — 故意禁用: 见上方决策记录. 多租户暂缓, 本拦截器保持休眠.
// 该 stub 无实际 SQL 注入逻辑, 且在代理 StatementHandler 链上会因缺 'delegate'
// getter 而崩溃 — 实装多租户前不得恢复 @Component.
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TenantInterceptor implements Interceptor {

    // Tables that should have tenant filtering (v3)
    private static final Set<String> TENANT_TABLES = Set.of(
        "users", "roles", "permissions", "user_roles", "role_permissions",
        "access_relations", "access_relations_history", "relation_types",
        "org_units", "classes", "user_student",
        "data_resources", "role_data_scopes",
        "entity_field_definitions", "entity_attribute_values",
        "msg_notifications", "msg_subscription_rules", "msg_templates",
        "outbox_events", "event_schemas", "channel_deliveries"
    );

    // Tables to skip (system-wide tables)
    private static final Set<String> SKIP_TABLES = Set.of(
        "tenants", "casbin_rule"
    );

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();

        // Only intercept SELECT, UPDATE, DELETE
        if (sqlCommandType != SqlCommandType.SELECT &&
            sqlCommandType != SqlCommandType.UPDATE &&
            sqlCommandType != SqlCommandType.DELETE) {
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // Check if SQL already has tenant_id condition (avoid double-injection)
        if (originalSql.contains("tenant_id")) {
            return invocation.proceed();
        }

        // Simple heuristic: only inject if SQL references a tenant-aware table
        boolean hasTenantTable = TENANT_TABLES.stream()
                .anyMatch(table -> originalSql.toLowerCase().contains(table));

        if (!hasTenantTable) {
            return invocation.proceed();
        }

        // For now, skip auto-injection to avoid breaking complex JOINs.
        // Tenant filtering is handled explicitly in service/mapper layer.
        // This interceptor serves as a safety net for simple queries.
        // TODO: Implement proper tenant SQL injection for simple queries

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
