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
 * MyBatis interceptor that automatically injects tenant_id filtering.
 * For SELECT/UPDATE/DELETE: appends WHERE tenant_id = ? condition.
 * For INSERT: handled by entity defaults (tenant_id column default = 1).
 *
 * NOTE: Uses string injection for tenant_id which is safe because
 * tenantId is always a Long from TenantContextHolder (server-side only).
 */
@Slf4j
// @Component — Disabled: this interceptor is a stub (no actual SQL injection yet).
// It crashes on proxied StatementHandler chains (no 'delegate' getter).
// Re-enable when actual tenant SQL injection is implemented.
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TenantInterceptor implements Interceptor {

    // Tables that should have tenant filtering
    private static final Set<String> TENANT_TABLES = Set.of(
        "users", "roles", "permissions", "user_roles", "role_permissions",
        "access_relations", "org_units", "classes", "students",
        "data_modules", "scope_item_types", "module_scope_item_types",
        "role_data_permissions_v5", "role_data_scope_items"
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
