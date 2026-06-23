package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.ScopePreset;
import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;

/**
 * Data Permission Interceptor (Scoped Roles) —— <b>瘦壳化 (T7)</b>。
 *
 * <p>MyBatis 拦截器, 向出站 SQL 注入参数化的行级数据权限过滤。每个角色的 scope 决定其
 * 数据过滤的 org root; 多角色条件 OR 合并。
 *
 * <p><b>职责边界</b>: 本类只负责拦截器自有机制 —— SQL 注入点定位 (injectFilterCondition)、
 * 参数位置绑定 (trailing-LIMIT 映射插入)、注解解析缓存、per-role effective-org 计算、
 * 多角色 OR 合并 + ALL 短路。<b>SQL compose 全部委托</b> {@link ScopeEvaluator#toSqlCondition}
 * (读路径统一 compose 真相源, 三正交轴: org anchor / subject-relation filter / type filter)。
 *
 * <p>每个角色: 读 {@link ScopeSpec} (PolicyService, 无配置→SELF) + 构建 {@link ResourceScopeMeta}
 * (注解 ⊕ moduleConfig), 计算该角色有效 org, 交 evaluator compose 出 {@link ScopeCondition}。
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
    private ScopeEvaluator scopeEvaluator;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private ResourceRelationRegistry resourceRelationRegistry;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!UserContextHolder.isDataPermissionEnabled()) {
            return invocation.proceed();
        }

        UserContext userContext = UserContextHolder.getContext();
        // userContext == null 仅出现在后台线程 (定时任务 / 异步 / 启动初始化), 这些不经
        // JwtAuthenticationFilter, 没有用户身份 → 故意 fail-open (系统操作需全量访问)。
        // 真实 HTTP 请求一定先过 filter 设上下文 (SecurityConfig anyRequest().authenticated()),
        // 不会落到 null 分支; 若要对某后台路径关数据权限, 用 isDataPermissionEnabled 开关而非依赖此处。
        // 超管同样跳过过滤 (全量数据)。
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

        boolean isInsert = mappedStatement.getSqlCommandType() == org.apache.ibatis.mapping.SqlCommandType.INSERT;
        Long tenantId = TenantContextHolder.getTenantId();

        // INSERT (R8 P3-INSERT 授权): 无 WHERE 可注入 → 改为"新行 owner_org ∈ 用户可写组织"前置校验。
        // 平台健壮性: insert 方法常无注解 (BaseMapper.insert), 故用 any-method 解析 (扫 mapper 任一
        // @DataPermission), 让 withInsertGuard 对接口级/方法级 mapper 一致生效。enforceInsertAuthz 内部
        // 再 gate isInsertGuarded (默认 false) → 未标注资源放行, 加 any-method 不引入新强制 (fail-safe)。
        if (isInsert) {
            DataPermission insAnno = getDataPermissionAnnotation(mapperId);
            if (insAnno == null) insAnno = resolveAnyMethodAnnotation(mapperId);
            if (insAnno != null && insAnno.enabled()) {
                DataModulePO mc = dynamicModuleService.getModuleConfig(tenantId, insAnno.module());
                if (mc != null && mc.getEnabled()) {
                    enforceInsertAuthz(statementHandler.getBoundSql(), insAnno, mc, userContext, tenantId);
                }
            }
            return invocation.proceed();
        }

        DataPermission dataPermission = getDataPermissionAnnotation(mapperId);
        if (dataPermission == null || !dataPermission.enabled()) {
            return invocation.proceed();
        }

        String moduleCode = dataPermission.module();

        // Check module config
        DataModulePO moduleConfig = dynamicModuleService.getModuleConfig(tenantId, moduleCode);
        if (moduleConfig == null || !moduleConfig.getEnabled()) {
            return invocation.proceed();
        }

        // 动作类: SELECT→READ, 其余 (UPDATE/DELETE) → WRITE。INSERT 已在上方处理。
        // 当前所有 seed 行 apply_to=BOTH, READ 与 WRITE 都命中同一行 → 行为等价。
        String actionClass = mappedStatement.getSqlCommandType()
                == org.apache.ibatis.mapping.SqlCommandType.SELECT ? "READ" : "WRITE";

        // Build scoped condition using scopedRoles
        ScopeCondition condition = buildScopedCondition(
                dataPermission, moduleConfig, userContext, tenantId, actionClass);

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
        for (ScopeCondition.Param param : condition.params) {
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
     * Build condition using scoped roles —— <b>瘦壳化 (T7)</b>: 每角色委托
     * {@link ScopeEvaluator#toSqlCondition} compose SQL, 拦截器只负责
     * (1) per-role effective-org 计算 (2) 多角色 OR 合并 (3) ALL 短路 (4) empty→deny。
     *
     * <p>各角色: 读 {@link ScopeSpec} (null→SELF) + 构建 {@link ResourceScopeMeta} (注解⊕moduleConfig),
     * 算出该角色有效 org (ORG_UNIT scope-type→角色 scope org; 否则用户主组织), 交给 evaluator。
     * 非空 role 条件 OR 合并; 全 ALL-unbounded → null (放行全量); 全 empty → "1 = 0" (deny)。
     */
    private ScopeCondition buildScopedCondition(
            DataPermission annotation, DataModulePO moduleConfig,
            UserContext userContext, Long tenantId, String actionClass) {

        List<UserContext.ScopedRoleInfo> scopedRoles = userContext.getScopedRoles();

        // Fallback: if no scopedRoles, route legacy roleIds through the same evaluator path.
        if (scopedRoles == null || scopedRoles.isEmpty()) {
            return buildLegacyCondition(annotation, moduleConfig, userContext, tenantId, actionClass);
        }

        String moduleCode = annotation.module();
        ResourceScopeMeta meta = buildMeta(annotation, moduleConfig);

        List<String> roleSqls = new ArrayList<>();
        ScopeCondition combined = new ScopeCondition();
        int globalParamIdx = 0;

        for (UserContext.ScopedRoleInfo sr : scopedRoles) {
            // 该角色×资源×动作类 的可组合规格; 无配置 → 安全降级 SELF (等价旧 coreScope==null→SELF)。
            ScopeSpec spec = dataPermissionPolicyService.getScopeSpec(
                    tenantId, sr.getRoleId(), moduleCode, actionClass);
            if (spec == null) {
                spec = ScopePreset.SELF.toSpec();
            }

            // ALL 短路 (等价旧 buildScopedCondition): scopeType==ALL 且 spec 为"全组织无界"
            // (orgAnchor==ALL 且无类型/关系过滤) → 整查询放行 (返回 null)。
            // 注意: scopeType==ORG_UNIT 但 anchor==ALL 不短路 — evaluator 产空 cond, 下方 skip,
            // 与旧 case ALL→null (该角色不贡献, 不释放整查询) 等价。
            if (ScopeType.ALL.equals(sr.getScopeType())
                    && spec.isOrgUnbounded()
                    && !spec.hasTypeFilter() && !spec.hasRelInclude() && !spec.hasRelExclude()) {
                return null; // No filter — see everything
            }

            // per-role 有效 org: ORG_UNIT scope-type → 角色 scope org; 否则用户主组织。
            Long effectiveOrgId;
            String effectiveOrgPath;
            if (ScopeType.ORG_UNIT.equals(sr.getScopeType())) {
                effectiveOrgId = sr.getScopeId();
                effectiveOrgPath = sr.getScopeOrgPath();
            } else {
                effectiveOrgId = userContext.getOrgUnitId();
                effectiveOrgPath = userContext.getOrgUnitPath();
            }

            // R3b 正解 (R4 enabled): plugin-dim resolve 改用 meta.resourceCode() (buildPluginDimCondition),
            // 不再"hasPluginDimGrant→withResourceType 注入 moduleCode" —— 那会污染同 spec 的非 PLUGIN_DIM
            // grant (COLUMN 资源被推 accessRelationSelect → DENY 丢)。resourceCode 与 resourceType 解耦后,
            // 多 grant 混 PLUGIN_DIM 与 org/creator 自然正确。meta 直传。
            ScopeCondition roleCond = scopeEvaluator.toSqlCondition(
                    spec, meta, userContext, effectiveOrgId, effectiveOrgPath, tenantId, globalParamIdx);

            if (roleCond != null && !roleCond.sql.isEmpty()) {
                roleSqls.add(roleCond.sql);
                combined.params.addAll(roleCond.params);
                globalParamIdx += roleCond.params.size();
            }
        }

        if (roleSqls.isEmpty()) {
            // No valid conditions → deny all
            combined.sql = "1 = 0";
            return combined;
        }

        if (roleSqls.size() == 1) {
            combined.sql = roleSqls.get(0);
        } else {
            combined.sql = "(" + String.join(" OR ", roleSqls) + ")";
        }

        return combined;
    }

    /**
     * Legacy path (no scopedRoles): 把每个 roleId 当作"ALL scope-type, 锚点=用户主组织"
     * 路由进同一 {@link ScopeEvaluator}, 与 scopedRoles 路径同语义。无配置 → SELF。
     *
     * <p>legacy 路径在真实 HTTP 请求中极少/从不命中 (JwtAuthenticationFilter 一定填 scopedRoles);
     * 仅作为 scopedRoles 为空时的兜底。改走 evaluator 换取删除旧 merge-semantics build* 方法,
     * dedup 收益。注: 旧 legacy 走 getMergedScope (跨角色合并取最宽) + hasSelfScope OR-creator,
     * 新路径改为逐角色 compose 后 OR 合并 — 对单一锚点结果一致, 多角色时新路径是各角色范围的并集
     * (等价或更安全的收窄), 且此路径几乎不被命中。
     */
    private ScopeCondition buildLegacyCondition(
            DataPermission annotation, DataModulePO moduleConfig,
            UserContext userContext, Long tenantId, String actionClass) {

        String moduleCode = annotation.module();
        ResourceScopeMeta meta = buildMeta(annotation, moduleConfig);

        // 无角色 → 单个 SELF 规格 (保留旧 getMergedScope(empty)→SELF 的"仅本人"收窄,
        // 不放宽为全量)。真实请求几乎不会落到此处 (无 scopedRoles 且无 roleIds)。
        List<Long> roleIds = userContext.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            roleIds = Collections.singletonList(-1L); // 占位, spec 强制 SELF 见下
        }

        List<String> roleSqls = new ArrayList<>();
        ScopeCondition combined = new ScopeCondition();
        int globalParamIdx = 0;
        boolean noRealRoles = userContext.getRoleIds() == null || userContext.getRoleIds().isEmpty();

        for (Long roleId : roleIds) {
            ScopeSpec spec = noRealRoles
                    ? ScopePreset.SELF.toSpec()
                    : dataPermissionPolicyService.getScopeSpec(tenantId, roleId, moduleCode, actionClass);
            if (spec == null) {
                spec = ScopePreset.SELF.toSpec();
            }

            // legacy 无 per-role scope → 锚点一律用户主组织; ALL-unbounded → 整查询放行。
            if (spec.isOrgUnbounded()
                    && !spec.hasTypeFilter() && !spec.hasRelInclude() && !spec.hasRelExclude()) {
                return null;
            }

            // R3b 正解 (R4): plugin-dim 用 resourceCode, 不再注入 resourceType (见核心路径注释)。
            ScopeCondition roleCond = scopeEvaluator.toSqlCondition(
                    spec, meta, userContext,
                    userContext.getOrgUnitId(), userContext.getOrgUnitPath(), tenantId, globalParamIdx);

            if (roleCond != null && !roleCond.sql.isEmpty()) {
                roleSqls.add(roleCond.sql);
                combined.params.addAll(roleCond.params);
                globalParamIdx += roleCond.params.size();
            }
        }

        if (roleSqls.isEmpty()) {
            combined.sql = "1 = 0";
            return combined;
        }
        combined.sql = roleSqls.size() == 1 ? roleSqls.get(0) : "(" + String.join(" OR ", roleSqls) + ")";
        return combined;
    }

    /**
     * 从 {@code resource_relations} 注册表 ⊕ {@code @DataPermission} 注解 ⊕ {@code moduleConfig}
     * 构建 {@link ResourceScopeMeta}。各字段来源 (Tier 1 R2.4 起):
     * <ul>
     *   <li>orgUnitField / creatorField / viaMembership —— <b>resource_relations 注册表</b> (唯一真相源;
     *       缺该模块 → fail-fast, 注解兜底已删)。</li>
     *   <li>tableAlias —— 注解 (sanitize)。</li>
     *   <li>resourceType —— annotation.resourceType (非空) ?? moduleConfig.resourceType。
     *       <b>不</b>兜底 moduleCode — 核心路径 hasResourceType() 选择必须与旧 buildSingleRoleCondition
     *       一致 (空→org-field, 非空→access_relation)。PLUGIN_DIM 的 moduleCode 由 buildPluginDimCondition
     *       直接用 {@code meta.resourceCode()} (R4; 不再注入 resourceType, 避免污染非 PLUGIN_DIM grant)。</li>
     *   <li>membershipSubjectColumn —— 注解 (Tier 2 再迁注册表)。</li>
     *   <li>typeField —— moduleConfig.typeField (sanitize; 为空则轴③禁用; Tier 2 再迁)。</li>
     * </ul>
     */
    private ResourceScopeMeta buildMeta(DataPermission annotation, DataModulePO moduleConfig) {
        String tableAlias = annotation.tableAlias().isEmpty() ? "" : sanitizeIdentifier(annotation.tableAlias());

        // Tier 1 (统一锚定 R2.4): 锚点 (orgUnitField / creatorField / viaMembership) 唯一来自
        // resource_relations 注册表; @DataPermission 注解兜底已删。forResource 缺该模块 = 锚点缺失
        // = 资源裸奔 → fail-fast。覆盖由 PluginDeclarationCoverageTest 构建期守护 (每个 @DataPermission
        // 模块必登锚点) + P3 懒加载保证运行期注册表已载, 故此异常实际不可达 (防御 contribution 漏登/写失败)。
        // null → coerce 成 "" (成员主体无列锚; 成员路径不消费 org/creator 列 — BuildMetaRegistryEquivalenceTest)。
        // tableAlias / membershipSubjectColumn / typeField 仍来自注解/data_resources (Tier 2 再迁)。
        ResourceRelationRegistry.DerivedAnchor a = resourceRelationRegistry.forResource(annotation.module())
                .orElseThrow(() -> new IllegalStateException(
                        "模块 " + annotation.module() + " 未注册 resource_relations 锚点 — @DataPermission 无注解兜底 " +
                        "(统一锚定 R2.4); 检查 PluginPackage.contribute() 是否登记该模块的 owner_org/creator 关系"));
        String orgField = a.orgUnitField() == null ? "" : sanitizeIdentifier(a.orgUnitField());
        String creatorField = a.creatorField() == null ? "" : sanitizeIdentifier(a.creatorField());
        boolean viaMembership = a.viaMembership();

        String membershipSubjectColumn = annotation.membershipSubjectColumn() == null
                ? null : sanitizeIdentifier(annotation.membershipSubjectColumn());
        String typeField = sanitizeIdentifier(moduleConfig.getTypeField());

        return new ResourceScopeMeta(
                tableAlias, orgField, creatorField,
                viaMembership, membershipSubjectColumn, typeField, annotation.module());
    }

    // ── R8 P3-INSERT 授权 ─────────────────────────────────────────────────────

    /**
     * INSERT 授权: 新行 owner_org ∈ 用户可写组织集才放行。设计见
     * docs/plans/2026-06-22-p3-insert-authz-design.md。
     *
     * <p>逐锚点语义: ALL / creator 关系 / SELF 主体 → 放行 (新行天然属创建者自己 / 无界);
     * owner_org 组织有界 (MY_ORG/RELATION/CUSTOM/PLUGIN_DIM) → owner_org 必 ∈ 可写组织。
     * <p><b>fail-safe</b>: 无 org 列 (viaMembership/PROVIDER/仅 creator) / 取不到值 / 异常 → 放行,
     * 绝不因插桩 bug 阻断合法 INSERT (最坏退回"漏拦"=现状)。越界才 throw。
     */
    private void enforceInsertAuthz(BoundSql boundSql, DataPermission annotation,
                                    DataModulePO moduleConfig, UserContext ctx, Long tenantId) {
        try {
            // R8 P3-INSERT 显式 opt-in: 仅 owner_org 标注 enforce_insert_scope 的 ownership 资源参与
            // (排除 target 语义资源如 inspection submission 的 org_unit_id=受检组织, 否则误拦)。
            if (!resourceRelationRegistry.isInsertGuarded(annotation.module())) return;
            ResourceScopeMeta meta = buildMeta(annotation, moduleConfig);
            String orgField = meta.orgUnitField();
            if (orgField == null || orgField.isEmpty()) return;   // 无 org 列锚 → 放行
            Long ownerOrg = extractColumnValue(boundSql, orgField);
            if (ownerOrg == null) return;                          // 取不到 owner_org → fail-safe 放行
            if (!isOrgWritable(annotation.module(), ctx, tenantId, ownerOrg)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "无权在该组织下创建记录 (org=" + ownerOrg + ", module=" + annotation.module() + ")");
            }
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[InsertAuthz] 校验异常, fail-safe 放行 (module={}): {}", annotation.module(), e.toString());
        }
    }

    /** 从 INSERT 参数实体读某列值 (snake_case 列 → 驼峰字段)，非数值/缺失 → null。 */
    private Long extractColumnValue(BoundSql boundSql, String column) {
        Object param = boundSql.getParameterObject();
        if (param == null) return null;
        String field = snakeToCamel(column);
        MetaObject mo = SystemMetaObject.forObject(param);
        if (!mo.hasGetter(field)) return null;
        Object v = mo.getValue(field);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v == null) return null;
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static String snakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : s.toCharArray()) {
            if (c == '_') { up = true; }
            else { sb.append(up ? Character.toUpperCase(c) : c); up = false; }
        }
        return sb.toString();
    }

    /**
     * owner_org 是否在用户对该资源的可写组织集内 (合成 org 元探针 org_units)。
     * 任一角色的 WRITE spec 含 ALL / creator 关系 / SELF 主体 → 放行; 否则各组织有界 grant
     * 经 ScopeEvaluator 产 org 条件 (orgField=id 探 org_units), OR 合并后探针 owner_org。
     */
    private boolean isOrgWritable(String module, UserContext ctx, Long tenantId, Long ownerOrg) {
        List<UserContext.ScopedRoleInfo> roles = ctx.getScopedRoles();
        if (roles == null || roles.isEmpty()) return true;        // legacy 路径 → fail-safe 放行
        // 合成 org 元: 探 org_units.id, 无别名; resourceCode=module (PLUGIN_DIM resolve 用)。
        ResourceScopeMeta orgMeta = new ResourceScopeMeta("", "id", "", false, "id", null, module);
        List<String> orgConds = new ArrayList<>();
        ScopeCondition combined = new ScopeCondition();
        int idx = 0;
        for (UserContext.ScopedRoleInfo sr : roles) {
            com.school.management.domain.access.model.valueobject.ScopeSpec spec =
                    dataPermissionPolicyService.getScopeSpec(tenantId, sr.getRoleId(), module, "WRITE");
            if (spec == null || spec.getRelationGrants() == null || spec.getRelationGrants().isEmpty()) {
                return true;   // 无配置 → SELF (creator) → 新行我即创建者 → 放行
            }
            for (com.school.management.domain.access.model.valueobject.RelationGrant g : spec.getRelationGrants()) {
                if (g.subject() == com.school.management.domain.access.model.SubjectScope.ALL) return true;
                if (com.school.management.domain.access.model.valueobject.RelationGrant.CREATOR.equals(g.relation())) return true;
                if (g.subject() == com.school.management.domain.access.model.SubjectScope.SELF) return true;
            }
            Long effOrg = ScopeType.ORG_UNIT.equals(sr.getScopeType()) ? sr.getScopeId() : ctx.getOrgUnitId();
            String effPath = ScopeType.ORG_UNIT.equals(sr.getScopeType()) ? sr.getScopeOrgPath() : ctx.getOrgUnitPath();
            ScopeCondition c = scopeEvaluator.toSqlCondition(spec, orgMeta, ctx, effOrg, effPath, tenantId, idx);
            if (c != null && !c.sql.isEmpty()) {
                orgConds.add(c.sql);
                combined.params.addAll(c.params);
                idx += c.params.size();
            }
        }
        if (orgConds.isEmpty()) return true;                       // 无可判定条件 → fail-safe 放行
        String where = orgConds.size() == 1 ? orgConds.get(0)
                : "(" + String.join(") OR (", orgConds) + ")";
        String sql = "SELECT EXISTS(SELECT 1 FROM org_units WHERE id = ? AND (" + where + ") AND deleted = 0)";
        Object[] args = new Object[combined.params.size() + 1];
        args[0] = ownerOrg;
        for (int i = 0; i < combined.params.size(); i++) args[i + 1] = combined.params.get(i).value;
        // 非 varargs 重载 (args, type): 顺序 args 在前, 避免 varargs 歧义 (亦便于 mock)。
        Boolean ok = jdbcTemplate.queryForObject(sql, args, Boolean.class);
        return Boolean.TRUE.equals(ok);
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

    /** mapperId → 解析后的 @DataPermission 注解缓存 (安全审计 B2: 避免重复反射 + 失败只 log 一次). */
    private final java.util.Map<String, java.util.Optional<DataPermission>> annotationCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private DataPermission getDataPermissionAnnotation(String mapperId) {
        return annotationCache
                .computeIfAbsent(mapperId, this::resolveDataPermissionAnnotation)
                .orElse(null);
    }

    /** mapperClassName → 该 mapper 任一方法/类上的 @DataPermission (R8 INSERT: insert 无注解时回退用)。 */
    private final java.util.Map<String, java.util.Optional<DataPermission>> anyMethodAnnotationCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 取 mapper "任一" @DataPermission (类级优先, 否则任一方法级)。供 INSERT 授权: BaseMapper.insert
     * 无自身注解, 但该 mapper 是数据权限管的资源 → 用它的模块做 INSERT 授权 (仍 gate isInsertGuarded)。
     * <p>同一 mapper 通常单一 module, 取首个即可。
     */
    private DataPermission resolveAnyMethodAnnotation(String mapperId) {
        int lastDot = mapperId.lastIndexOf('.');
        String className = lastDot > 0 ? mapperId.substring(0, lastDot) : mapperId;
        return anyMethodAnnotationCache.computeIfAbsent(className, cn -> {
            try {
                Class<?> mapperClass = Class.forName(cn);
                DataPermission classAnno = mapperClass.getAnnotation(DataPermission.class);
                if (classAnno != null) return java.util.Optional.of(classAnno);
                for (Method m : mapperClass.getMethods()) {
                    DataPermission ma = m.getAnnotation(DataPermission.class);
                    if (ma != null) return java.util.Optional.of(ma);
                }
                return java.util.Optional.empty();
            } catch (Exception e) {
                log.error("[DataPermission] any-method 注解解析失败 mapper={}: {}", cn, e.getMessage());
                return java.util.Optional.empty();
            }
        }).orElse(null);
    }

    private java.util.Optional<DataPermission> resolveDataPermissionAnnotation(String mapperId) {
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
                        return java.util.Optional.of(methodAnnotation);
                    }
                }
            }

            return java.util.Optional.ofNullable(classAnnotation);
        } catch (Exception e) {
            // 安全审计 B2 (2026-05-20): 注解解析失败不再静默 debug — 升到 ERROR.
            // 失败意味着无法判定该 mapper 是否需数据权限过滤; mapper class 已由
            // MyBatis 加载, 正常不会失败. 真失败需人工排查 (classloader/插件热加载).
            // (彻底 fail-closed 需启动期预解析全部注解, 留后续.)
            log.error("[DataPermission] 注解解析失败, mapper={} — 无法判定该查询是否需数据权限过滤: {}",
                    mapperId, e.getMessage(), e);
            return java.util.Optional.empty();
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
        // Alias missing — strip "alias." occurrences, but ONLY at identifier boundary:
        // 裸 replace("s.","") 会殃及子查询里以该别名结尾的别名 (如 "us." 含 "s." → "us.id"→"uid")。
        // 负向后顾确保只剥独立的 "<alias>.",不碰 "u<alias>." 这类 (R3c PROVIDER 子查询暴露此潜伏 bug)。
        return filterSql.replaceAll(
                "(?<![A-Za-z0-9_])" + java.util.regex.Pattern.quote(san) + "\\.", "");
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
}
