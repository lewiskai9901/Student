package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限拦截器 (V5)
 * MyBatis拦截器，自动在SQL中注入数据权限过滤条件
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    @Autowired
    private DataModuleRegistry moduleRegistry;

    @Autowired
    private DataPermissionService dataPermissionService;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否启用数据权限
        if (!UserContextHolder.isDataPermissionEnabled()) {
            return invocation.proceed();
        }

        // 获取用户上下文
        UserContext userContext = UserContextHolder.getContext();
        if (userContext == null) {
            return invocation.proceed();
        }

        // 超级管理员跳过
        if (userContext.isSuperAdmin()) {
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        String mapperId = mappedStatement.getId();

        // 获取注解
        DataPermission dataPermission = getDataPermissionAnnotation(mapperId);
        if (dataPermission == null || !dataPermission.enabled()) {
            return invocation.proceed();
        }

        // 获取模块配置
        String moduleCode = dataPermission.module();
        DataModuleRegistry.ModuleConfig moduleConfig = moduleRegistry.getModuleConfig(moduleCode);
        if (moduleConfig == null || !moduleConfig.isEnableDataPermission()) {
            return invocation.proceed();
        }

        // 获取合并后的数据范围
        MergedDataScope mergedScope = dataPermissionService.getMergedScope(
                userContext.getRoleIds(), moduleCode);

        if (mergedScope == null || mergedScope.isAllScope()) {
            return invocation.proceed();
        }

        // 构建过滤条件
        String filterCondition = buildFilterCondition(mergedScope, dataPermission, moduleConfig, userContext);
        if (filterCondition == null || filterCondition.isEmpty()) {
            return invocation.proceed();
        }

        // 修改SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();
        String newSql = injectFilterCondition(originalSql, filterCondition, dataPermission.tableAlias());

        // 替换SQL
        metaObject.setValue("delegate.boundSql.sql", newSql);

        if (log.isDebugEnabled()) {
            log.debug("Data permission filter applied. Module: {}, Scope: {}, Original: {}, New: {}",
                    moduleCode, mergedScope.getEffectiveScope(), originalSql, newSql);
        }

        return invocation.proceed();
    }

    /**
     * 获取Mapper方法上的DataPermission注解
     */
    private DataPermission getDataPermissionAnnotation(String mapperId) {
        try {
            int lastDot = mapperId.lastIndexOf('.');
            String className = mapperId.substring(0, lastDot);
            String methodName = mapperId.substring(lastDot + 1);

            Class<?> mapperClass = Class.forName(className);

            // 首先检查类级别注解
            DataPermission classAnnotation = mapperClass.getAnnotation(DataPermission.class);

            // 检查方法级别注解
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    DataPermission methodAnnotation = method.getAnnotation(DataPermission.class);
                    if (methodAnnotation != null) {
                        return methodAnnotation;
                    }
                }
            }

            return classAnnotation;
        } catch (Exception e) {
            log.debug("Failed to get DataPermission annotation for {}: {}", mapperId, e.getMessage());
            return null;
        }
    }

    /**
     * 构建过滤条件SQL片段
     */
    private String buildFilterCondition(MergedDataScope mergedScope, DataPermission annotation,
                                        DataModuleRegistry.ModuleConfig moduleConfig, UserContext userContext) {
        DataScope scope = mergedScope.getEffectiveScope();
        StringBuilder condition = new StringBuilder();
        String alias = annotation.tableAlias().isEmpty() ? "" : annotation.tableAlias() + ".";

        switch (scope) {
            case DEPARTMENT:
                // 仅本部门
                String orgField = moduleConfig.getOrgUnitField();
                if (orgField == null) orgField = annotation.orgUnitField();
                condition.append(alias).append(orgField).append(" = ").append(userContext.getOrgUnitId());
                break;

            case DEPARTMENT_AND_BELOW:
                // 本部门及下级
                orgField = moduleConfig.getOrgUnitField();
                if (orgField == null) orgField = annotation.orgUnitField();
                // 使用org_unit_path进行前缀匹配
                if (userContext.getOrgUnitPath() != null) {
                    condition.append("(")
                            .append(alias).append(orgField).append(" IN (")
                            .append("SELECT id FROM org_units WHERE path LIKE '")
                            .append(userContext.getOrgUnitPath()).append("%'")
                            .append("))");
                } else {
                    condition.append(alias).append(orgField).append(" = ").append(userContext.getOrgUnitId());
                }
                break;

            case CUSTOM:
                // 自定义范围
                condition.append(buildCustomCondition(mergedScope, annotation, moduleConfig, alias));
                break;

            case SELF:
                // 仅本人
                String creatorField = moduleConfig.getCreatorField();
                if (creatorField == null) creatorField = annotation.creatorField();
                condition.append(alias).append(creatorField).append(" = ").append(userContext.getUserId());
                break;

            default:
                return null;
        }

        // 如果同时有SELF权限，添加OR条件
        if (mergedScope.isHasSelfScope() && scope != DataScope.SELF) {
            String creatorField = moduleConfig.getCreatorField();
            if (creatorField == null) creatorField = annotation.creatorField();
            condition.insert(0, "(");
            condition.append(" OR ").append(alias).append(creatorField).append(" = ").append(userContext.getUserId());
            condition.append(")");
        }

        return condition.toString();
    }

    /**
     * 构建自定义范围条件
     */
    private String buildCustomCondition(MergedDataScope mergedScope, DataPermission annotation,
                                        DataModuleRegistry.ModuleConfig moduleConfig, String alias) {
        StringBuilder condition = new StringBuilder();
        boolean hasCondition = false;

        // 组织单元条件
        Set<Long> orgUnitIds = mergedScope.getOrgUnitIds();
        if (!orgUnitIds.isEmpty()) {
            String orgField = moduleConfig.getOrgUnitField();
            if (orgField == null) orgField = annotation.orgUnitField();

            Set<Long> withChildren = mergedScope.getOrgUnitsWithChildren();
            Set<Long> withoutChildren = orgUnitIds.stream()
                    .filter(id -> !withChildren.contains(id))
                    .collect(Collectors.toSet());

            if (!withoutChildren.isEmpty()) {
                condition.append(alias).append(orgField).append(" IN (")
                        .append(withoutChildren.stream().map(String::valueOf).collect(Collectors.joining(",")))
                        .append(")");
                hasCondition = true;
            }

            for (Long orgId : withChildren) {
                if (hasCondition) condition.append(" OR ");
                condition.append(alias).append(orgField).append(" IN (")
                        .append("SELECT id FROM org_units WHERE path LIKE (")
                        .append("SELECT CONCAT(path, '%') FROM org_units WHERE id = ").append(orgId)
                        .append("))");
                hasCondition = true;
            }
        }

        // 班级条件
        Set<Long> classIds = mergedScope.getClassIds();
        if (!classIds.isEmpty()) {
            String classField = moduleConfig.getClassField();
            if (classField == null) classField = annotation.classField();

            if (hasCondition) condition.append(" OR ");
            condition.append(alias).append(classField).append(" IN (")
                    .append(classIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                    .append(")");
            hasCondition = true;
        }

        if (!hasCondition) {
            // 无有效条件，返回永假条件防止数据泄露
            return "1 = 0";
        }

        return "(" + condition.toString() + ")";
    }

    /**
     * 将过滤条件注入SQL
     */
    private String injectFilterCondition(String sql, String filterCondition, String tableAlias) {
        // 简单实现：在WHERE之后或FROM之后添加条件
        String upperSql = sql.toUpperCase();

        int whereIndex = upperSql.lastIndexOf(" WHERE ");
        if (whereIndex > 0) {
            // 已有WHERE，添加AND条件
            int insertPos = whereIndex + 7; // " WHERE ".length()
            return sql.substring(0, insertPos) + "(" + filterCondition + ") AND " + sql.substring(insertPos);
        }

        // 查找GROUP BY, ORDER BY, LIMIT等位置
        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        int insertPos = sql.length();
        if (groupByIndex > 0) insertPos = Math.min(insertPos, groupByIndex);
        if (orderByIndex > 0) insertPos = Math.min(insertPos, orderByIndex);
        if (limitIndex > 0) insertPos = Math.min(insertPos, limitIndex);

        // 在适当位置插入WHERE条件
        return sql.substring(0, insertPos) + " WHERE " + filterCondition + sql.substring(insertPos);
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
        // 可配置属性
    }
}
