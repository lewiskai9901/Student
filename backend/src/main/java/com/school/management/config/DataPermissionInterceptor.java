package com.school.management.config;

import com.school.management.util.DataPermissionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

/**
 * MyBatis 数据权限拦截器
 * 自动在 SELECT 语句中追加数据权限过滤条件
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否启用数据权限
        if (!DataPermissionContextHolder.isEnabled()) {
            return invocation.proceed();
        }

        String dataScopeSql = DataPermissionContextHolder.getDataScopeSql();
        if (!StringUtils.hasText(dataScopeSql)) {
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 只处理 SELECT 语句
        if (mappedStatement.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        // 获取原始 SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // 追加数据权限条件
        String newSql = addDataScopeCondition(originalSql, dataScopeSql);

        // 替换 SQL
        setFieldValue(boundSql, "sql", newSql);

        log.debug("Original SQL: {}", originalSql);
        log.debug("Modified SQL: {}", newSql);

        return invocation.proceed();
    }

    /**
     * 在 SQL 中追加数据权限条件
     */
    private String addDataScopeCondition(String originalSql, String dataScopeSql) {
        String sql = originalSql.trim();
        String lowerSql = sql.toLowerCase();

        // 查找关键字位置
        int orderIndex = lowerSql.lastIndexOf(" order by ");
        int groupIndex = lowerSql.lastIndexOf(" group by ");
        int limitIndex = lowerSql.lastIndexOf(" limit ");

        // 确定插入位置（在 ORDER BY, GROUP BY, LIMIT 之前）
        int insertIndex = sql.length();
        if (limitIndex > 0) insertIndex = Math.min(insertIndex, limitIndex);
        if (orderIndex > 0) insertIndex = Math.min(insertIndex, orderIndex);
        if (groupIndex > 0) insertIndex = Math.min(insertIndex, groupIndex);

        // 在适当位置插入条件
        String beforeInsert = sql.substring(0, insertIndex);
        String afterInsert = sql.substring(insertIndex);

        return beforeInsert + " " + dataScopeSql + " " + afterInsert;
    }

    /**
     * 通过反射设置字段值
     */
    private void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            log.error("Failed to set field value: {}", fieldName, e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可配置属性
    }
}
