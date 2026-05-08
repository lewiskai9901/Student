package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Permission Interceptor (Scoped Roles)
 *
 * MyBatis interceptor that injects parameterized data permission filtering.
 * Supports scoped role assignments: each role's scope determines the org root
 * for data filtering. Multiple role conditions are OR-combined.
 *
 * <p>此拦截器是 {@link DataScope} (粗粒度 RBAC 视图) 与
 * {@code access_relations} 表 (细粒度 ReBAC 视图) 的桥接,详见
 * {@code backend/docs/design/access/ADR-001-datascope-as-access-relation-macro.md}。
 *
 * <p>实际行为:从角色读 MergedDataScope, 按 effectiveScope 决定走哪条路径:
 * <ul>
 *   <li>静态 scope (SELF / DEPARTMENT / DEPARTMENT_AND_BELOW) → 直接拼 WHERE 子句</li>
 *   <li>module 配置了 resourceType → access_relations 子查询 (buildAccessRelationCondition)</li>
 *   <li>插件维度 (如 BY_MAJOR) → PluginDataScopeRouter (buildPluginDimCondition)</li>
 *   <li>CUSTOM → role_custom_scope (buildCustomCondition)</li>
 * </ul>
 * 二者职责清晰,不互斥; DataScope 是 AccessRelation 的宏观快捷表达。
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.school.management.application.access.DynamicModuleService dynamicModuleService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private DataPermissionPolicyService dataPermissionPolicyService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private PluginDataScopeRouter pluginDataScopeRouter;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!UserContextHolder.isDataPermissionEnabled()) {
            return invocation.proceed();
        }

        UserContext userContext = UserContextHolder.getContext();
        if (userContext == null || userContext.isSuperAdmin()) {
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // Unwrap JDK proxy layers created by Plugin.wrap() in multi-interceptor chains
        while (metaObject.hasGetter("h")) {
            Object plugin = metaObject.getValue("h");
            MetaObject pluginMeta = SystemMetaObject.forObject(plugin);
            if (pluginMeta.hasGetter("target")) {
                statementHandler = (StatementHandler) pluginMeta.getValue("target");
                metaObject = SystemMetaObject.forObject(statementHandler);
            } else {
                break;
            }
        }

        if (!metaObject.hasGetter("delegate.mappedStatement")) {
            return invocation.proceed();
        }

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        String mapperId = mappedStatement.getId();

        // INSERT statements have no WHERE clause — appending filter produces invalid SQL.
        // Ownership for new rows is enforced by the application layer, not by row-level filters.
        if (mappedStatement.getSqlCommandType() == org.apache.ibatis.mapping.SqlCommandType.INSERT) {
            return invocation.proceed();
        }

        DataPermission dataPermission = getDataPermissionAnnotation(mapperId);
        if (dataPermission == null || !dataPermission.enabled()) {
            return invocation.proceed();
        }

        Long tenantId = TenantContextHolder.getTenantId();
        String moduleCode = dataPermission.module();

        // Check module config
        DataModulePO moduleConfig = dynamicModuleService.getModuleConfig(tenantId, moduleCode);
        if (moduleConfig == null || !moduleConfig.getEnabled()) {
            return invocation.proceed();
        }

        // Build scoped condition using scopedRoles
        ParameterizedCondition condition = buildScopedCondition(
                dataPermission, moduleConfig, userContext, tenantId);

        if (condition == null || condition.sql.isEmpty()) {
            return invocation.proceed();
        }

        // Inject into SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();
        // If the SQL does not actually use the configured tableAlias (e.g. generated
        // COUNT(*) queries from BaseMapper don't alias the table), strip the alias prefix
        // from the filter so we don't reference a non-existent alias.
        String effectiveFilter = stripAliasIfNotInSql(condition.sql, originalSql,
                dataPermission.tableAlias());
        String newSql = injectFilterCondition(originalSql, effectiveFilter);

        // Set new SQL
        metaObject.setValue("delegate.boundSql.sql", newSql);

        // Our filter is injected BEFORE ORDER BY / LIMIT clause (see injectFilterCondition),
        // so the `?` placeholders sit between the original WHERE's `?`s and any trailing
        // LIMIT `?, ?` added by MyBatis-Plus PaginationInterceptor.
        //
        // Parameter mappings must match positional order:  [origWhere..., filter..., LIMIT...]
        // If we append at the end, our values would bind to LIMIT slots and vice versa →
        // SQL syntax error when a tree_path string ends up in LIMIT.
        //
        // Fix: find the first LIMIT mapping (if any) and insert ours before it; otherwise append.
        Configuration configuration = mappedStatement.getConfiguration();
        // BoundSql.getParameterMappings() may return an unmodifiable list — copy to mutable
        // ArrayList, mutate, then write back via MetaObject (the underlying field is private).
        List<ParameterMapping> mappings = new ArrayList<>(boundSql.getParameterMappings());
        // Count `?` placeholders that come AFTER our injection point (i.e. in ORDER BY / LIMIT).
        // Parameter mappings are positional — those trailing `?`s bind to the LAST N entries.
        // So we insert before them to keep the order [origWhere..., filter..., trailing...].
        int trailingPlaceholders = countPlaceholdersAfterInjection(newSql, effectiveFilter);
        int insertAt = trailingPlaceholders > 0 && trailingPlaceholders <= mappings.size()
                ? mappings.size() - trailingPlaceholders : -1;
        for (AdditionalParam param : condition.params) {
            ParameterMapping.Builder pmBuilder = new ParameterMapping.Builder(
                    configuration, param.property, param.javaType);
            pmBuilder.jdbcType(param.jdbcType);
            ParameterMapping pm = pmBuilder.build();
            if (insertAt < 0) {
                mappings.add(pm);
            } else {
                mappings.add(insertAt, pm);
                insertAt++;
            }
            boundSql.setAdditionalParameter(param.property, param.value);
        }
        // Write the mutated list back into BoundSql's private field
        SystemMetaObject.forObject(boundSql).setValue("parameterMappings", mappings);

        if (log.isDebugEnabled()) {
            log.debug("Data permission filter applied: module={}, params={}",
                    moduleCode, condition.params.size());
        }

        return invocation.proceed();
    }

    /**
     * Build condition using scoped roles.
     * For each active scoped role, resolve its DataPermission for the module,
     * then generate a scoped filter. All roles' filters are OR-combined.
     * If any role has ALL scope + ALL DataScope → short-circuit to no filter.
     */
    private ParameterizedCondition buildScopedCondition(
            DataPermission annotation, DataModulePO moduleConfig,
            UserContext userContext, Long tenantId) {

        List<UserContext.ScopedRoleInfo> scopedRoles = userContext.getScopedRoles();

        // Fallback: if no scopedRoles, use legacy merged scope approach
        if (scopedRoles == null || scopedRoles.isEmpty()) {
            return buildLegacyCondition(annotation, moduleConfig, userContext, tenantId);
        }

        String moduleCode = annotation.module();
        List<String> roleSqls = new ArrayList<>();
        ParameterizedCondition combinedCond = new ParameterizedCondition();
        int globalParamIdx = 0;

        for (UserContext.ScopedRoleInfo sr : scopedRoles) {
            // Get this role's raw scope code (could be core enum OR plugin dim code like BY_MAJOR)
            String rawScopeCode = dataPermissionPolicyService.getScopeCodeForRole(
                    tenantId, sr.getRoleId(), moduleCode);

            DataScope coreScope = DataScope.fromCodeStrict(rawScopeCode);

            // Plugin dim path: rawScopeCode is non-null but not a core enum
            if (rawScopeCode != null && coreScope == null) {
                String resourceType = annotation.resourceType();
                if (resourceType.isEmpty() && moduleConfig.getResourceType() != null) {
                    resourceType = moduleConfig.getResourceType();
                }
                ParameterizedCondition pluginCond = buildPluginDimCondition(
                        rawScopeCode, annotation, resourceType, userContext, globalParamIdx);
                if (pluginCond != null && !pluginCond.sql.isEmpty()) {
                    roleSqls.add(pluginCond.sql);
                    combinedCond.params.addAll(pluginCond.params);
                    globalParamIdx += pluginCond.params.size();
                }
                continue;
            }

            // Core path: rawScopeCode is null (unconfigured) OR a core enum
            DataScope dataScope = coreScope;
            if (dataScope == null) {
                // No data permission configured for this role+module → default SELF
                dataScope = DataScope.SELF;
            }

            // Short-circuit: ALL scope + ALL DataScope → no filter needed
            if (ScopeType.ALL.equals(sr.getScopeType()) && dataScope == DataScope.ALL) {
                return null; // No filter
            }

            // Determine the effective org root for this role
            String effectiveOrgPath;
            Long effectiveOrgId;
            if (ScopeType.ORG_UNIT.equals(sr.getScopeType())) {
                effectiveOrgPath = sr.getScopeOrgPath();
                effectiveOrgId = sr.getScopeId();
            } else {
                // ALL scope type - use user's primary org as fallback
                effectiveOrgPath = userContext.getOrgUnitPath();
                effectiveOrgId = userContext.getOrgUnitId();
            }

            // Build condition for this role
            ParameterizedCondition roleCond = buildSingleRoleCondition(
                    dataScope, annotation, moduleConfig, userContext, tenantId,
                    effectiveOrgId, effectiveOrgPath, globalParamIdx);

            if (roleCond != null && !roleCond.sql.isEmpty()) {
                roleSqls.add(roleCond.sql);
                combinedCond.params.addAll(roleCond.params);
                globalParamIdx += roleCond.params.size();
            }
        }

        if (roleSqls.isEmpty()) {
            // No valid conditions → deny all
            combinedCond.sql = "1 = 0";
            return combinedCond;
        }

        if (roleSqls.size() == 1) {
            combinedCond.sql = roleSqls.get(0);
        } else {
            combinedCond.sql = "(" + String.join(" OR ", roleSqls) + ")";
        }

        return combinedCond;
    }

    /**
     * Build condition for a plugin-contributed data scope dimension (e.g. BY_MAJOR).
     *
     * Routes to {@link PluginDataScopeRouter} which looks up data_scope_dims.resolver_type
     * and invokes the resolver's resolve(userId, resourceType).
     *
     * Return semantics:
     *   null from router       → degrade to SELF (safe: creator filter)
     *   empty list from router → deny all ("1 = 0")
     *   non-empty list         → {alias}id IN (?, ?, ...)
     */
    private ParameterizedCondition buildPluginDimCondition(
            String dimCode, DataPermission annotation, String resourceType,
            UserContext userContext, int paramOffset) {

        String alias = annotation.tableAlias().isEmpty() ? "" : sanitizeIdentifier(annotation.tableAlias()) + ".";
        ParameterizedCondition cond = new ParameterizedCondition();

        List<Long> ids = pluginDataScopeRouter.resolve(dimCode, userContext.getUserId(), resourceType);

        if (ids == null) {
            // Dim not found or resolver unavailable → safe degrade to SELF
            log.warn("[DataPermission] plugin dim '{}' unavailable, degrading to SELF", dimCode);
            String creatorField = sanitizeIdentifier(
                    annotation.creatorField() == null || annotation.creatorField().isEmpty()
                            ? "created_by" : annotation.creatorField());
            cond.sql = alias + creatorField + " = ?";
            cond.addParam("_dp_pluginSelf_" + paramOffset, userContext.getUserId(),
                    Long.class, JdbcType.BIGINT);
            return cond;
        }

        if (ids.isEmpty()) {
            // Resolver explicitly says "no access"
            cond.sql = "1 = 0";
            return cond;
        }

        // Numeric IDs are safe to inline (no injection surface). Keeps the param
        // mapping list simple given that id counts can be large.
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        cond.sql = alias + "id IN (" + csv + ")";
        return cond;
    }

    /**
     * Build condition for a single role with a specific DataScope and org root.
     */
    private ParameterizedCondition buildSingleRoleCondition(
            DataScope scope, DataPermission annotation, DataModulePO moduleConfig,
            UserContext userContext, Long tenantId,
            Long orgId, String orgPath, int paramOffset) {

        String alias = annotation.tableAlias().isEmpty() ? "" : sanitizeIdentifier(annotation.tableAlias()) + ".";
        ParameterizedCondition cond = new ParameterizedCondition();

        String resourceType = annotation.resourceType();
        if (resourceType.isEmpty() && moduleConfig.getResourceType() != null) {
            resourceType = moduleConfig.getResourceType();
        }

        // If module uses access_relations, build access relation condition
        if (!resourceType.isEmpty()) {
            return buildAccessRelationCondition(resourceType, annotation, scope,
                    userContext, tenantId, orgId, orgPath, paramOffset);
        }

        // Otherwise use org field filtering
        String orgField = sanitizeIdentifier(
                moduleConfig.getOrgUnitField() != null ? moduleConfig.getOrgUnitField() : annotation.orgUnitField());
        String creatorField = sanitizeIdentifier(
                moduleConfig.getCreatorField() != null ? moduleConfig.getCreatorField() : annotation.creatorField());

        switch (scope) {
            case ALL:
                return null; // No filter for this role

            case DEPARTMENT:
                if (orgId != null) {
                    cond.sql = alias + orgField + " = ?";
                    cond.addParam("_dp_orgId_" + paramOffset, orgId, Long.class, JdbcType.BIGINT);
                }
                break;

            case DEPARTMENT_AND_BELOW:
                if (orgPath != null) {
                    cond.sql = "(" + alias + orgField + " IN (" +
                            "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?))";
                    cond.addParam("_dp_tenantId_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
                    cond.addParam("_dp_orgPath_" + paramOffset, orgPath + "%", String.class, JdbcType.VARCHAR);
                } else if (orgId != null) {
                    cond.sql = alias + orgField + " = ?";
                    cond.addParam("_dp_orgId_" + paramOffset, orgId, Long.class, JdbcType.BIGINT);
                }
                break;

            case SELF:
                cond.sql = alias + creatorField + " = ?";
                cond.addParam("_dp_creatorId_" + paramOffset, userContext.getUserId(), Long.class, JdbcType.BIGINT);
                break;

            case CUSTOM:
                // CUSTOM scope: fall back to legacy merged scope approach for this role
                MergedDataScope mergedScope = dataPermissionPolicyService.getMergedScope(
                        tenantId, Collections.singletonList(Long.valueOf(paramOffset)), annotation.module());
                if (mergedScope != null) {
                    return buildCustomCondition(mergedScope, alias, orgField, tenantId, paramOffset);
                }
                break;

            default:
                return null;
        }

        return cond;
    }

    /**
     * Build condition using access_relations subquery with scoped org root.
     */
    private ParameterizedCondition buildAccessRelationCondition(
            String resourceType, DataPermission annotation, DataScope scope,
            UserContext userContext, Long tenantId,
            Long orgId, String orgPath, int paramOffset) {

        String alias = annotation.tableAlias().isEmpty() ? "" : sanitizeIdentifier(annotation.tableAlias()) + ".";
        ParameterizedCondition cond = new ParameterizedCondition();

        StringBuilder sb = new StringBuilder();
        sb.append(alias).append("id IN (")
          .append("SELECT ar.resource_id FROM access_relations ar WHERE ar.resource_type = ? AND ar.tenant_id = ? AND ar.deleted = 0 AND (")
          .append("(ar.subject_type = 'user' AND ar.subject_id = ?)");

        cond.addParam("_dp_resType_" + paramOffset, resourceType, String.class, JdbcType.VARCHAR);
        cond.addParam("_dp_tenantId_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
        cond.addParam("_dp_userId_" + paramOffset, userContext.getUserId(), Long.class, JdbcType.BIGINT);

        // Use the scoped org root (not user's primary org)
        if (orgPath != null) {
            sb.append(" OR (ar.subject_type = 'org_unit' AND ar.subject_id IN (")
              .append("SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0))");
            cond.addParam("_dp_tenantId2_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
            cond.addParam("_dp_orgPath_" + paramOffset, orgPath + "%", String.class, JdbcType.VARCHAR);
        } else if (orgId != null) {
            sb.append(" OR (ar.subject_type = 'org_unit' AND ar.subject_id = ?)");
            cond.addParam("_dp_orgId_" + paramOffset, orgId, Long.class, JdbcType.BIGINT);
        }

        sb.append("))");

        // Also add orgUnitField direct filter with OR
        String orgField = sanitizeIdentifier(annotation.orgUnitField());
        if (orgField != null && orgId != null &&
                (scope == DataScope.DEPARTMENT || scope == DataScope.DEPARTMENT_AND_BELOW)) {
            sb.insert(0, "(");
            if (scope == DataScope.DEPARTMENT_AND_BELOW && orgPath != null) {
                sb.append(" OR ").append(alias).append(orgField)
                  .append(" IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)");
                cond.addParam("_dp_tenantId3_" + paramOffset, tenantId, Long.class, JdbcType.BIGINT);
                cond.addParam("_dp_orgPath2_" + paramOffset, orgPath + "%", String.class, JdbcType.VARCHAR);
            } else {
                sb.append(" OR ").append(alias).append(orgField).append(" = ?");
                cond.addParam("_dp_orgDirect_" + paramOffset, orgId, Long.class, JdbcType.BIGINT);
            }
            sb.append(")");
        }

        cond.sql = sb.toString();
        return cond;
    }

    /**
     * Legacy condition builder: used when no scopedRoles are present (backward compatibility).
     */
    private ParameterizedCondition buildLegacyCondition(
            DataPermission annotation, DataModulePO moduleConfig,
            UserContext userContext, Long tenantId) {

        String moduleCode = annotation.module();
        MergedDataScope mergedScope = dataPermissionPolicyService.getMergedScope(
                tenantId, userContext.getRoleIds(), moduleCode);

        if (mergedScope == null || mergedScope.isAllScope()) {
            return null;
        }

        String resourceType = annotation.resourceType();
        if (resourceType.isEmpty() && moduleConfig.getResourceType() != null) {
            resourceType = moduleConfig.getResourceType();
        }

        if (!resourceType.isEmpty()) {
            return buildAccessRelationCondition(resourceType, annotation, mergedScope.getEffectiveScope(),
                    userContext, tenantId, userContext.getOrgUnitId(), userContext.getOrgUnitPath(), 0);
        }

        return buildLegacyOrgFilterCondition(mergedScope, annotation, moduleConfig, userContext, tenantId);
    }

    /**
     * Legacy org filter condition (unchanged from V6).
     */
    private ParameterizedCondition buildLegacyOrgFilterCondition(
            MergedDataScope mergedScope, DataPermission annotation,
            DataModulePO moduleConfig, UserContext userContext, Long tenantId) {

        DataScope scope = mergedScope.getEffectiveScope();
        String alias = annotation.tableAlias().isEmpty() ? "" : sanitizeIdentifier(annotation.tableAlias()) + ".";
        ParameterizedCondition cond = new ParameterizedCondition();

        String orgField = sanitizeIdentifier(
                moduleConfig.getOrgUnitField() != null ? moduleConfig.getOrgUnitField() : annotation.orgUnitField());
        String creatorField = sanitizeIdentifier(
                moduleConfig.getCreatorField() != null ? moduleConfig.getCreatorField() : annotation.creatorField());

        switch (scope) {
            case DEPARTMENT:
                cond.sql = alias + orgField + " = ?";
                cond.addParam("_dp_orgId", userContext.getOrgUnitId(), Long.class, JdbcType.BIGINT);
                break;

            case DEPARTMENT_AND_BELOW:
                if (userContext.getOrgUnitPath() != null) {
                    cond.sql = "(" + alias + orgField + " IN (" +
                            "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?))";
                    cond.addParam("_dp_tenantId", tenantId, Long.class, JdbcType.BIGINT);
                    cond.addParam("_dp_orgPath", userContext.getOrgUnitPath() + "%", String.class, JdbcType.VARCHAR);
                } else {
                    cond.sql = alias + orgField + " = ?";
                    cond.addParam("_dp_orgId", userContext.getOrgUnitId(), Long.class, JdbcType.BIGINT);
                }
                break;

            case CUSTOM:
                cond = buildCustomCondition(mergedScope, alias, orgField, tenantId, 0);
                break;

            case SELF:
                cond.sql = alias + creatorField + " = ?";
                cond.addParam("_dp_creatorId", userContext.getUserId(), Long.class, JdbcType.BIGINT);
                break;

            default:
                return null;
        }

        // If scope != SELF but hasSelfScope, OR with creator condition
        if (mergedScope.isHasSelfScope() && scope != DataScope.SELF && !cond.sql.isEmpty()) {
            cond.sql = "(" + cond.sql + " OR " + alias + creatorField + " = ?)";
            cond.addParam("_dp_selfCreator", userContext.getUserId(), Long.class, JdbcType.BIGINT);
        }

        return cond;
    }

    /**
     * Build CUSTOM scope condition (parameterized)
     */
    private ParameterizedCondition buildCustomCondition(
            MergedDataScope mergedScope, String alias, String orgField, Long tenantId, int baseParamOffset) {

        ParameterizedCondition cond = new ParameterizedCondition();
        StringBuilder sb = new StringBuilder();
        boolean hasCondition = false;
        int paramIdx = baseParamOffset;

        Set<Long> orgUnitIds = mergedScope.getOrgUnitIds();
        if (!orgUnitIds.isEmpty()) {
            Set<Long> withChildren = mergedScope.getOrgUnitsWithChildren();
            Set<Long> withoutChildren = orgUnitIds.stream()
                    .filter(id -> !withChildren.contains(id))
                    .collect(Collectors.toSet());

            if (!withoutChildren.isEmpty()) {
                String placeholders = withoutChildren.stream().map(id -> "?").collect(Collectors.joining(","));
                sb.append(alias).append(orgField).append(" IN (").append(placeholders).append(")");
                for (Long id : withoutChildren) {
                    cond.addParam("_dp_customOrg_" + paramIdx++, id, Long.class, JdbcType.BIGINT);
                }
                hasCondition = true;
            }

            for (Long orgId : withChildren) {
                if (hasCondition) sb.append(" OR ");
                sb.append(alias).append(orgField).append(" IN (")
                  .append("SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE (")
                  .append("SELECT CONCAT(tree_path, '%') FROM org_units WHERE id = ?))");
                cond.addParam("_dp_tenantOrg_" + paramIdx, tenantId, Long.class, JdbcType.BIGINT);
                cond.addParam("_dp_childOrg_" + paramIdx, orgId, Long.class, JdbcType.BIGINT);
                paramIdx++;
                hasCondition = true;
            }
        }

        if (!hasCondition) {
            cond.sql = "1 = 0";
            return cond;
        }

        cond.sql = "(" + sb + ")";
        return cond;
    }

    /**
     * Inject filter condition into SQL.
     *
     * Appends the filter at the END of the existing WHERE clause (or creates WHERE before
     * GROUP BY/ORDER BY/LIMIT if none). Appending — rather than prepending — means the
     * filter's `?` placeholders come AFTER the original query's `?`s, so parameter mappings
     * can simply be appended to the existing list in the same order.
     */
    private String injectFilterCondition(String sql, String filterCondition) {
        String upperSql = sql.toUpperCase();

        int groupByIndex = upperSql.indexOf(" GROUP BY ");
        int orderByIndex = upperSql.indexOf(" ORDER BY ");
        int limitIndex = upperSql.indexOf(" LIMIT ");

        int tailPos = sql.length();
        if (groupByIndex > 0) tailPos = Math.min(tailPos, groupByIndex);
        if (orderByIndex > 0) tailPos = Math.min(tailPos, orderByIndex);
        if (limitIndex > 0) tailPos = Math.min(tailPos, limitIndex);

        int whereIndex = upperSql.lastIndexOf(" WHERE ", tailPos);
        if (whereIndex > 0) {
            return sql.substring(0, tailPos) + " AND (" + filterCondition + ")" + sql.substring(tailPos);
        }

        return sql.substring(0, tailPos) + " WHERE " + filterCondition + sql.substring(tailPos);
    }

    private DataPermission getDataPermissionAnnotation(String mapperId) {
        try {
            int lastDot = mapperId.lastIndexOf('.');
            String className = mapperId.substring(0, lastDot);
            String methodName = mapperId.substring(lastDot + 1);

            Class<?> mapperClass = Class.forName(className);
            DataPermission classAnnotation = mapperClass.getAnnotation(DataPermission.class);

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
     * If the original SQL does not reference the configured tableAlias (e.g. "SELECT COUNT(*)
     * FROM user_student WHERE ..." has no alias), strip "{alias}." prefix from the filter so
     * the injected SQL doesn't reference an undefined alias.
     */
    private String stripAliasIfNotInSql(String filterSql, String originalSql, String alias) {
        if (filterSql == null || alias == null || alias.isEmpty()) return filterSql;
        String san = sanitizeIdentifier(alias);
        if (san == null || san.isEmpty()) return filterSql;
        // Does original SQL actually declare this alias? Look for patterns "<alias>." /
        // " <alias> " / "AS <alias>". Be lenient — if any appear, keep the alias.
        String upperOrig = " " + originalSql.toUpperCase() + " ";
        String a = san.toUpperCase();
        if (upperOrig.contains(" " + a + ".") ||
            upperOrig.contains(" " + a + " ") ||
            upperOrig.contains(" AS " + a + " ") ||
            upperOrig.contains(" AS " + a + ",") ||
            upperOrig.contains("(" + a + ".")) {
            return filterSql;
        }
        // Alias missing — strip "alias." occurrences
        return filterSql.replace(san + ".", "");
    }

    /**
     * Count `?` placeholders in {@code newSql} that come AFTER our injected {@code filterSql}.
     * The injected filter itself is skipped — only placeholders in the trailing ORDER BY /
     * LIMIT region are counted. These typically come from MyBatis-Plus pagination.
     */
    private int countPlaceholdersAfterInjection(String newSql, String filterSql) {
        if (newSql == null || filterSql == null || filterSql.isEmpty()) return 0;
        int idx = newSql.indexOf(filterSql);
        if (idx < 0) return 0;
        int tailStart = idx + filterSql.length();
        int count = 0;
        for (int i = tailStart; i < newSql.length(); i++) {
            if (newSql.charAt(i) == '?') count++;
        }
        return count;
    }

    /**
     * Sanitize SQL identifier to prevent injection via column/table names
     */
    private String sanitizeIdentifier(String identifier) {
        if (identifier == null) return null;
        return identifier.replaceAll("[^a-zA-Z0-9_]", "");
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

    /**
     * Holds a parameterized SQL condition and its parameters
     */
    private static class ParameterizedCondition {
        String sql = "";
        final List<AdditionalParam> params = new ArrayList<>();

        void addParam(String property, Object value, Class<?> javaType, JdbcType jdbcType) {
            AdditionalParam p = new AdditionalParam();
            p.property = property;
            p.value = value;
            p.javaType = javaType;
            p.jdbcType = jdbcType;
            params.add(p);
        }
    }

    private static class AdditionalParam {
        String property;
        Object value;
        Class<?> javaType;
        JdbcType jdbcType;
    }
}
