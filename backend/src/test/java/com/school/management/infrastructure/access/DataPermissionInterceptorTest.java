package com.school.management.infrastructure.access;

import com.school.management.application.access.DynamicModuleService;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DataPermissionInterceptor}.
 *
 * <p>The interceptor is security-critical: it injects row-level WHERE clauses into
 * outbound SQL based on the current user's scoped roles. These tests exercise:
 * <ul>
 *   <li>Pure SQL-string helpers ({@code injectFilterCondition}, {@code stripAliasIfNotInSql},
 *       {@code sanitizeIdentifier}, {@code countPlaceholdersAfterInjection}) via reflection.</li>
 *   <li>Annotation resolution ({@code getDataPermissionAnnotation} /
 *       {@code resolveDataPermissionAnnotation}) against real test mapper classes.</li>
 *   <li>Scope-condition builders ({@code buildScopedCondition}, {@code buildSingleRoleCondition},
 *       {@code buildAccessRelationCondition}, {@code buildPluginDimCondition},
 *       {@code buildCustomCondition}, {@code buildLegacyOrgFilterCondition}) with real
 *       {@link UserContext} input for ALL / DEPARTMENT / DEPARTMENT_AND_BELOW / SELF / CUSTOM.</li>
 *   <li>The {@code intercept(Invocation)} early-return branches.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataPermissionInterceptorTest {

    @Mock
    private DynamicModuleService dynamicModuleService;

    @Mock
    private DataPermissionPolicyService dataPermissionPolicyService;

    @Mock
    private PluginDataScopeRouter pluginDataScopeRouter;

    private DataPermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new DataPermissionInterceptor();
        ReflectionTestUtils.setField(interceptor, "dynamicModuleService", dynamicModuleService);
        ReflectionTestUtils.setField(interceptor, "dataPermissionPolicyService", dataPermissionPolicyService);
        ReflectionTestUtils.setField(interceptor, "pluginDataScopeRouter", pluginDataScopeRouter);
        UserContextHolder.clear();
        UserContextHolder.enableDataPermission();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // Test fixtures: mapper classes / annotations
    // ------------------------------------------------------------------

    @DataPermission(module = "student", tableAlias = "s", orgUnitField = "org_unit_id", creatorField = "created_by")
    interface AnnotatedMapper {
        List<Object> selectList();

        @DataPermission(module = "method_module", tableAlias = "m")
        List<Object> selectWithMethodAnnotation();
    }

    interface PlainMapper {
        List<Object> selectList();
    }

    private DataPermission stubAnnotation() {
        return AnnotatedMapper.class.getAnnotation(DataPermission.class);
    }

    private DataModulePO moduleConfig(boolean enabled, String resourceType) {
        DataModulePO po = new DataModulePO();
        po.setModuleCode("student");
        po.setEnabled(enabled);
        po.setResourceType(resourceType);
        po.setOrgUnitField("org_unit_id");
        po.setCreatorField("created_by");
        return po;
    }

    private UserContext.ScopedRoleInfo scopedRole(Long roleId, String scopeType, Long scopeId, String orgPath) {
        return UserContext.ScopedRoleInfo.builder()
                .roleId(roleId)
                .scopeType(scopeType)
                .scopeId(scopeId)
                .scopeOrgPath(orgPath)
                .build();
    }

    private UserContext userWithScopedRoles(List<UserContext.ScopedRoleInfo> roles) {
        return UserContext.builder()
                .userId(42L)
                .username("teacher")
                .orgUnitId(100L)
                .orgUnitPath("1.10.100.")
                .roleIds(roles == null ? Collections.emptyList()
                        : roles.stream().map(UserContext.ScopedRoleInfo::getRoleId).toList())
                .tenantId(1L)
                .scopedRoles(roles)
                .build();
    }

    // ==================================================================
    // sanitizeIdentifier
    // ==================================================================
    @Nested
    @DisplayName("sanitizeIdentifier — SQL 标识符净化防注入")
    class SanitizeIdentifier {

        @Test
        @DisplayName("null 输入返回 null")
        void nullReturnsNull() {
            String result = ReflectionTestUtils.invokeMethod(interceptor, "sanitizeIdentifier", (Object) null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("纯净标识符原样保留")
        void cleanIdentifierUnchanged() {
            String result = ReflectionTestUtils.invokeMethod(interceptor, "sanitizeIdentifier", "org_unit_id");
            assertThat(result).isEqualTo("org_unit_id");
        }

        @Test
        @DisplayName("剥离注入字符 (空格/分号/引号/括号)")
        void stripsInjectionChars() {
            String result = ReflectionTestUtils.invokeMethod(
                    interceptor, "sanitizeIdentifier", "name; DROP TABLE users--");
            assertThat(result).isEqualTo("nameDROPTABLEusers");
        }

        @Test
        @DisplayName("保留数字与下划线，去除其余符号")
        void keepsAlnumAndUnderscore() {
            String result = ReflectionTestUtils.invokeMethod(interceptor, "sanitizeIdentifier", "t1.col-2");
            assertThat(result).isEqualTo("t1col2");
        }
    }

    // ==================================================================
    // injectFilterCondition
    // ==================================================================
    @Nested
    @DisplayName("injectFilterCondition — 过滤条件注入 SQL")
    class InjectFilterCondition {

        private String inject(String sql, String filter) {
            return ReflectionTestUtils.invokeMethod(interceptor, "injectFilterCondition", sql, filter);
        }

        @Test
        @DisplayName("已有 WHERE → 用 AND (...) 追加")
        void appendsToExistingWhere() {
            String out = inject("SELECT * FROM s WHERE s.deleted = 0", "s.org_unit_id = ?");
            assertThat(out).isEqualTo("SELECT * FROM s WHERE s.deleted = 0 AND (s.org_unit_id = ?)");
        }

        @Test
        @DisplayName("无 WHERE → 新建 WHERE 子句")
        void createsWhereWhenAbsent() {
            String out = inject("SELECT * FROM s", "s.org_unit_id = ?");
            assertThat(out).isEqualTo("SELECT * FROM s WHERE s.org_unit_id = ?");
        }

        @Test
        @DisplayName("过滤条件插在 ORDER BY 之前")
        void insertsBeforeOrderBy() {
            String out = inject("SELECT * FROM s WHERE s.deleted = 0 ORDER BY s.id", "s.org_unit_id = ?");
            assertThat(out).isEqualTo(
                    "SELECT * FROM s WHERE s.deleted = 0 AND (s.org_unit_id = ?) ORDER BY s.id");
        }

        @Test
        @DisplayName("过滤条件插在 GROUP BY 之前")
        void insertsBeforeGroupBy() {
            String out = inject("SELECT count(*) FROM s WHERE s.deleted = 0 GROUP BY s.type", "s.org_unit_id = ?");
            assertThat(out).isEqualTo(
                    "SELECT count(*) FROM s WHERE s.deleted = 0 AND (s.org_unit_id = ?) GROUP BY s.type");
        }

        @Test
        @DisplayName("过滤条件插在 LIMIT 之前")
        void insertsBeforeLimit() {
            String out = inject("SELECT * FROM s WHERE s.deleted = 0 LIMIT 10", "s.org_unit_id = ?");
            assertThat(out).isEqualTo(
                    "SELECT * FROM s WHERE s.deleted = 0 AND (s.org_unit_id = ?) LIMIT 10");
        }

        @Test
        @DisplayName("无 WHERE 但有 ORDER BY → WHERE 插在 ORDER BY 前")
        void createsWhereBeforeOrderBy() {
            String out = inject("SELECT * FROM s ORDER BY s.id", "s.org_unit_id = ?");
            assertThat(out).isEqualTo("SELECT * FROM s WHERE s.org_unit_id = ? ORDER BY s.id");
        }

        @Test
        @DisplayName("同时存在 ORDER BY 与 LIMIT → 取最靠前的位置")
        void picksEarliestTailClause() {
            String out = inject("SELECT * FROM s WHERE s.deleted = 0 ORDER BY s.id LIMIT 5", "s.x = ?");
            assertThat(out).isEqualTo(
                    "SELECT * FROM s WHERE s.deleted = 0 AND (s.x = ?) ORDER BY s.id LIMIT 5");
        }
    }

    // ==================================================================
    // stripAliasIfNotInSql
    // ==================================================================
    @Nested
    @DisplayName("stripAliasIfNotInSql — SQL 未声明别名时剥离别名前缀")
    class StripAliasIfNotInSql {

        private String strip(String filter, String sql, String alias) {
            return ReflectionTestUtils.invokeMethod(interceptor, "stripAliasIfNotInSql", filter, sql, alias);
        }

        @Test
        @DisplayName("filter 为 null → 原样返回 null")
        void nullFilterReturnsNull() {
            assertThat(strip(null, "SELECT * FROM s", "s")).isNull();
        }

        @Test
        @DisplayName("别名为空字符串 → 不剥离")
        void emptyAliasNoStrip() {
            assertThat(strip("s.org_unit_id = ?", "SELECT * FROM user_student", ""))
                    .isEqualTo("s.org_unit_id = ?");
        }

        @Test
        @DisplayName("SQL 含 'alias.' 引用 → 保留别名")
        void keepsAliasWhenReferencedWithDot() {
            String out = strip("s.org_unit_id = ?", "SELECT s.id FROM user_student s", "s");
            assertThat(out).isEqualTo("s.org_unit_id = ?");
        }

        @Test
        @DisplayName("SQL 用 ' alias ' 声明 → 保留别名")
        void keepsAliasWhenDeclaredWithSpaces() {
            String out = strip("s.org_unit_id = ?", "SELECT id FROM user_student s WHERE x = 1", "s");
            assertThat(out).isEqualTo("s.org_unit_id = ?");
        }

        @Test
        @DisplayName("SQL 用 'AS alias' 声明 → 保留别名")
        void keepsAliasWhenDeclaredWithAs() {
            String out = strip("s.org_unit_id = ?", "SELECT id FROM user_student AS s WHERE x = 1", "s");
            assertThat(out).isEqualTo("s.org_unit_id = ?");
        }

        @Test
        @DisplayName("生成的 COUNT(*) 查询无别名 → 剥离 'alias.' 前缀")
        void stripsAliasFromAliaslessCountQuery() {
            String out = strip("s.org_unit_id = ?", "SELECT COUNT(*) FROM user_student WHERE deleted = 0", "s");
            assertThat(out).isEqualTo("org_unit_id = ?");
        }
    }

    // ==================================================================
    // countPlaceholdersAfterInjection
    // ==================================================================
    @Nested
    @DisplayName("countPlaceholdersAfterInjection — 统计注入点之后的占位符")
    class CountPlaceholdersAfterInjection {

        private int count(String newSql, String filter) {
            Integer r = ReflectionTestUtils.invokeMethod(
                    interceptor, "countPlaceholdersAfterInjection", newSql, filter);
            return r == null ? -1 : r;
        }

        @Test
        @DisplayName("注入后存在 LIMIT 占位符 → 返回对应数量")
        void countsTrailingLimitPlaceholders() {
            int n = count("SELECT * FROM s WHERE a = ? AND (s.org = ?) LIMIT ?, ?", "s.org = ?");
            assertThat(n).isEqualTo(2);
        }

        @Test
        @DisplayName("注入后无尾部占位符 → 0")
        void zeroWhenNoTrailing() {
            int n = count("SELECT * FROM s WHERE a = ? AND (s.org = ?)", "s.org = ?");
            assertThat(n).isZero();
        }

        @Test
        @DisplayName("过滤片段未找到 → 0")
        void zeroWhenFilterNotFound() {
            int n = count("SELECT * FROM s WHERE a = ?", "s.missing = ?");
            assertThat(n).isZero();
        }

        @Test
        @DisplayName("空过滤片段 → 0")
        void zeroWhenEmptyFilter() {
            assertThat(count("SELECT * FROM s WHERE a = ?", "")).isZero();
        }
    }

    // ==================================================================
    // getDataPermissionAnnotation / resolveDataPermissionAnnotation
    // ==================================================================
    @Nested
    @DisplayName("getDataPermissionAnnotation — 注解解析")
    class GetDataPermissionAnnotation {

        private DataPermission resolve(String mapperId) {
            return ReflectionTestUtils.invokeMethod(interceptor, "getDataPermissionAnnotation", mapperId);
        }

        @Test
        @DisplayName("类上有 @DataPermission → 返回类级注解")
        void resolvesClassLevelAnnotation() {
            DataPermission ann = resolve(AnnotatedMapper.class.getName() + ".selectList");
            assertThat(ann).isNotNull();
            assertThat(ann.module()).isEqualTo("student");
            assertThat(ann.tableAlias()).isEqualTo("s");
        }

        @Test
        @DisplayName("方法上的 @DataPermission 优先于类级注解")
        void methodAnnotationOverridesClassLevel() {
            DataPermission ann = resolve(AnnotatedMapper.class.getName() + ".selectWithMethodAnnotation");
            assertThat(ann).isNotNull();
            assertThat(ann.module()).isEqualTo("method_module");
            assertThat(ann.tableAlias()).isEqualTo("m");
        }

        @Test
        @DisplayName("Mapper 类无 @DataPermission → 返回 null")
        void plainMapperReturnsNull() {
            assertThat(resolve(PlainMapper.class.getName() + ".selectList")).isNull();
        }

        @Test
        @DisplayName("不存在的类名 → 解析失败返回 null (fail-open, 已记录 ERROR)")
        void unknownClassReturnsNull() {
            assertThat(resolve("com.school.does.not.Exist.someMethod")).isNull();
        }

        @Test
        @DisplayName("结果被缓存 — 二次解析仍一致")
        void resultIsCached() {
            String mapperId = AnnotatedMapper.class.getName() + ".selectList";
            DataPermission first = resolve(mapperId);
            DataPermission second = resolve(mapperId);
            assertThat(first).isSameAs(second);
        }
    }

    // ==================================================================
    // buildScopedCondition (via reflection, with real UserContext)
    // ==================================================================
    @Nested
    @DisplayName("buildScopedCondition — 按 scoped roles 生成过滤条件")
    class BuildScopedCondition {

        private Object build(DataPermission ann, DataModulePO module, UserContext ctx, Long tenantId) {
            return ReflectionTestUtils.invokeMethod(interceptor, "buildScopedCondition",
                    ann, module, ctx, tenantId);
        }

        private String sqlOf(Object cond) {
            return (String) ReflectionTestUtils.getField(cond, "sql");
        }

        @SuppressWarnings("unchecked")
        private List<Object> paramsOf(Object cond) {
            return (List<Object>) ReflectionTestUtils.getField(cond, "params");
        }

        @Test
        @DisplayName("ALL scope + ALL DataScope → 短路返回 null (不过滤)")
        void allScopeShortCircuits() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(1L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(1L), anyString()))
                    .thenReturn(DataScope.ALL.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNull();
        }

        @Test
        @DisplayName("DEPARTMENT scope → '别名.org字段 = ?' 单参数")
        void departmentScopeBuildsEqualsCondition() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(2L, ScopeType.ORG_UNIT, 200L, "1.10.200.")));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(2L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.org_unit_id = ?");
            assertThat(paramsOf(cond)).hasSize(1);
        }

        @Test
        @DisplayName("DEPARTMENT_AND_BELOW scope → tree_path LIKE 子查询")
        void departmentAndBelowBuildsTreePathSubquery() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(3L, ScopeType.ORG_UNIT, 300L, "1.10.300.")));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(3L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT_AND_BELOW.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).contains("org_units").contains("tree_path LIKE ?");
            assertThat(paramsOf(cond)).hasSize(2);
        }

        @Test
        @DisplayName("SELF scope → 'creator 字段 = ?' 当前用户")
        void selfScopeBuildsCreatorCondition() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(4L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(4L), anyString()))
                    .thenReturn(DataScope.SELF.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
            assertThat(paramsOf(cond)).hasSize(1);
        }

        @Test
        @DisplayName("角色未配置 scope (返回 null) → 默认降级 SELF")
        void unconfiguredRoleDefaultsToSelf() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(5L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(5L), anyString()))
                    .thenReturn(null);

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
        }

        @Test
        @DisplayName("CUSTOM scope → 调 getMergedScope 并用真实 roleId (非 paramOffset)")
        void customScopeUsesRealRoleId() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(7L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(7L), anyString()))
                    .thenReturn(DataScope.CUSTOM.getCode());

            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student")
                    .effectiveScope(DataScope.CUSTOM)
                    .build();
            merged.getMergedScopeItems().put("ORG_UNIT", new HashSet<>(Set.of(901L, 902L)));
            when(dataPermissionPolicyService.getMergedScope(eq(1L), eq(Collections.singletonList(7L)), anyString()))
                    .thenReturn(merged);

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).contains("org_unit_id IN");
            // verify it used the real roleId, not a param offset
            verify(dataPermissionPolicyService)
                    .getMergedScope(eq(1L), eq(Collections.singletonList(7L)), anyString());
        }

        @Test
        @DisplayName("插件维度 scope (BY_MAJOR) → 路由 PluginDataScopeRouter, id IN (...)")
        void pluginDimScopeBuildsInClause() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(8L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(8L), anyString()))
                    .thenReturn("BY_MAJOR");
            when(pluginDataScopeRouter.resolve(eq("BY_MAJOR"), anyLong(), anyString()))
                    .thenReturn(List.of(11L, 22L, 33L));

            Object cond = build(stubAnnotation(), moduleConfig(true, "student"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.id IN (11,22,33)");
        }

        @Test
        @DisplayName("插件维度 resolver 返回 null → 安全降级 SELF (creator 过滤)")
        void pluginDimNullResolverDegradesToSelf() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(9L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(9L), anyString()))
                    .thenReturn("BY_MAJOR");
            when(pluginDataScopeRouter.resolve(anyString(), anyLong(), anyString()))
                    .thenReturn(null);

            Object cond = build(stubAnnotation(), moduleConfig(true, "student"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
        }

        @Test
        @DisplayName("插件维度 resolver 返回空列表 → 拒绝所有 '1 = 0'")
        void pluginDimEmptyResolverDeniesAll() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(10L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(10L), anyString()))
                    .thenReturn("BY_MAJOR");
            when(pluginDataScopeRouter.resolve(anyString(), anyLong(), anyString()))
                    .thenReturn(Collections.emptyList());

            Object cond = build(stubAnnotation(), moduleConfig(true, "student"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("1 = 0");
        }

        @Test
        @DisplayName("resourceType 非空 → 走 access_relations 子查询")
        void resourceTypeBuildsAccessRelationSubquery() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(11L, ScopeType.ORG_UNIT, 400L, "1.10.400.")));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(11L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, "student"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).contains("access_relations").contains("ar.resource_type = ?");
        }

        @Test
        @DisplayName("多角色 → 各自条件 OR 组合")
        void multipleRolesAreOrCombined() {
            UserContext ctx = userWithScopedRoles(List.of(
                    scopedRole(20L, ScopeType.ORG_UNIT, 500L, "1.10.500."),
                    scopedRole(21L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(20L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT.getCode());
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(21L), anyString()))
                    .thenReturn(DataScope.SELF.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).startsWith("(").contains(" OR ").endsWith(")");
            assertThat(paramsOf(cond)).hasSize(2);
        }

        @Test
        @DisplayName("无有效条件 → 拒绝所有 '1 = 0'")
        void noValidConditionDeniesAll() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(30L, ScopeType.ALL, 0L, null)));
            // ALL scopeType + ALL DataScope would short-circuit; use a role that yields no SQL:
            // DEPARTMENT scope with null orgId (ALL scopeType, user has no orgUnitId)
            UserContext noOrgCtx = UserContext.builder()
                    .userId(42L).orgUnitId(null).orgUnitPath(null)
                    .tenantId(1L)
                    .scopedRoles(List.of(scopedRole(30L, ScopeType.ALL, 0L, null)))
                    .build();
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(30L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT.getCode());

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), noOrgCtx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("1 = 0");
        }

        @Test
        @DisplayName("无 scopedRoles → 走 legacy 路径 (getMergedScope)")
        void emptyScopedRolesFallsBackToLegacy() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).orgUnitId(100L).orgUnitPath("1.10.100.")
                    .roleIds(List.of(99L))
                    .tenantId(1L)
                    .scopedRoles(Collections.emptyList())
                    .build();
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student")
                    .effectiveScope(DataScope.DEPARTMENT)
                    .build();
            when(dataPermissionPolicyService.getMergedScope(eq(1L), eq(List.of(99L)), anyString()))
                    .thenReturn(merged);

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.org_unit_id = ?");
        }

        @Test
        @DisplayName("legacy 路径 mergedScope 为 ALL → 返回 null (不过滤)")
        void legacyAllScopeReturnsNull() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).orgUnitId(100L).roleIds(List.of(99L)).tenantId(1L)
                    .scopedRoles(null)
                    .build();
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student").effectiveScope(DataScope.ALL).build();
            when(dataPermissionPolicyService.getMergedScope(anyLong(), any(), anyString()))
                    .thenReturn(merged);

            Object cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNull();
        }
    }

    // ==================================================================
    // buildMembershipCondition — user 资源按 member 关系派生归属过滤
    // ==================================================================
    @Nested
    @DisplayName("buildMembershipCondition — viaMembership 用户归属子查询")
    class BuildMembershipCondition {

        @DataPermission(module = "user", tableAlias = "u", viaMembership = true)
        interface MembershipMapper {
            List<Object> selectList();
        }

        private DataPermission membershipAnnotation() {
            return MembershipMapper.class.getAnnotation(DataPermission.class);
        }

        private Object build(DataPermission ann, DataModulePO module, UserContext ctx, Long tenantId) {
            return ReflectionTestUtils.invokeMethod(interceptor, "buildScopedCondition",
                    ann, module, ctx, tenantId);
        }

        private String sqlOf(Object cond) {
            return (String) ReflectionTestUtils.getField(cond, "sql");
        }

        @SuppressWarnings("unchecked")
        private List<Object> paramsOf(Object cond) {
            return (List<Object>) ReflectionTestUtils.getField(cond, "params");
        }

        @Test
        @DisplayName("ALL scope → 短路 null,不过滤")
        void allScopeShortCircuits() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(1L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(1L), anyString()))
                    .thenReturn(DataScope.ALL.getCode());
            Object cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNull();
        }

        @Test
        @DisplayName("DEPARTMENT → u.id IN (SELECT ar.subject_id ... member ... ar.resource_id = ?)")
        void departmentBuildsMembershipSubquery() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(2L, ScopeType.ORG_UNIT, 200L, "1.10.200.")));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(2L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT.getCode());
            Object cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond))
                    .startsWith("u.id IN (")
                    .contains("SELECT ar.subject_id FROM access_relations ar")
                    .contains("ar.relation = 'member'")
                    .contains("ar.resource_type = 'org_unit'")
                    .contains("ar.subject_type = 'user'")
                    .contains("ar.resource_id = ?");
            // positional params: [ar.tenant_id, ar.resource_id]
            assertThat(paramsOf(cond)).hasSize(2);
        }

        @Test
        @DisplayName("DEPARTMENT_AND_BELOW → ar.resource_id IN (org_units tree_path 子查询)")
        void departmentAndBelowBuildsTreePathSubquery() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(3L, ScopeType.ORG_UNIT, 300L, "1.10.300.")));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(3L), anyString()))
                    .thenReturn(DataScope.DEPARTMENT_AND_BELOW.getCode());
            Object cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond))
                    .contains("SELECT ar.subject_id FROM access_relations ar")
                    .contains("ar.resource_id IN (")
                    .contains("tree_path LIKE ?");
            // positional params: [ar.tenant_id, org tenant, org path]
            assertThat(paramsOf(cond)).hasSize(3);
        }

        @Test
        @DisplayName("SELF → u.id = ? (用户本人)")
        void selfScopeFiltersToSelf() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(4L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(4L), anyString()))
                    .thenReturn(DataScope.SELF.getCode());
            Object cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("u.id = ?");
            assertThat(paramsOf(cond)).hasSize(1);
        }

        @Test
        @DisplayName("CUSTOM 无配置组织 → 拒绝所有 1 = 0")
        void customWithoutOrgsDeniesAll() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(5L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(5L), anyString()))
                    .thenReturn(DataScope.CUSTOM.getCode());
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("user").effectiveScope(DataScope.CUSTOM).build();
            when(dataPermissionPolicyService.getMergedScope(eq(1L), eq(Collections.singletonList(5L)), anyString()))
                    .thenReturn(merged);
            Object cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("1 = 0");
        }

        @Test
        @DisplayName("CUSTOM 含组织 → ar.resource_id IN (内联 org ids)")
        void customWithOrgsInlinesIds() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(6L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeCodeForRole(eq(1L), eq(6L), anyString()))
                    .thenReturn(DataScope.CUSTOM.getCode());
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("user").effectiveScope(DataScope.CUSTOM).build();
            merged.getMergedScopeItems().put("ORG_UNIT", new HashSet<>(Set.of(77L)));
            when(dataPermissionPolicyService.getMergedScope(eq(1L), eq(Collections.singletonList(6L)), anyString()))
                    .thenReturn(merged);
            Object cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond))
                    .contains("ar.resource_id IN (77)")
                    .contains("SELECT ar.subject_id FROM access_relations ar");
            // only the ar.tenant_id param is bound (org ids inlined)
            assertThat(paramsOf(cond)).hasSize(1);
        }
    }

    // ==================================================================
    // buildCustomCondition (legacy custom builder, exercised directly)
    // ==================================================================
    @Nested
    @DisplayName("buildCustomCondition — CUSTOM 范围条件构建")
    class BuildCustomCondition {

        private Object buildCustom(MergedDataScope merged) {
            return ReflectionTestUtils.invokeMethod(interceptor, "buildCustomCondition",
                    merged, "s.", "org_unit_id", 1L, 0);
        }

        private String sqlOf(Object cond) {
            return (String) ReflectionTestUtils.getField(cond, "sql");
        }

        @Test
        @DisplayName("无组织单元 → 拒绝所有 '1 = 0'")
        void emptyOrgUnitsDeniesAll() {
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student").effectiveScope(DataScope.CUSTOM).build();
            Object cond = buildCustom(merged);
            assertThat(sqlOf(cond)).isEqualTo("1 = 0");
        }

        @Test
        @DisplayName("不含子级的组织 → IN (?, ?) 占位符")
        void orgUnitsWithoutChildrenBuildInClause() {
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student").effectiveScope(DataScope.CUSTOM).build();
            merged.getMergedScopeItems().put("ORG_UNIT", new HashSet<>(Set.of(11L, 22L)));
            Object cond = buildCustom(merged);
            assertThat(sqlOf(cond)).contains("org_unit_id IN (").startsWith("(").endsWith(")");
        }

        @Test
        @DisplayName("含子级的组织 → tree_path 递归子查询")
        void orgUnitsWithChildrenBuildTreePathSubquery() {
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student").effectiveScope(DataScope.CUSTOM).build();
            merged.getMergedScopeItems().put("ORG_UNIT", new HashSet<>(Set.of(33L)));
            merged.getOrgUnitsWithChildren().add(33L);
            Object cond = buildCustom(merged);
            assertThat(sqlOf(cond)).contains("tree_path LIKE").contains("org_units");
        }
    }

    // ==================================================================
    // buildLegacyOrgFilterCondition — hasSelfScope OR creator
    // ==================================================================
    @Nested
    @DisplayName("buildLegacyOrgFilterCondition — legacy org 过滤 + hasSelfScope")
    class BuildLegacyOrgFilterCondition {

        private Object build(MergedDataScope merged, UserContext ctx) {
            return ReflectionTestUtils.invokeMethod(interceptor, "buildLegacyOrgFilterCondition",
                    merged, stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
        }

        private String sqlOf(Object cond) {
            return (String) ReflectionTestUtils.getField(cond, "sql");
        }

        @Test
        @DisplayName("DEPARTMENT + hasSelfScope → OR 创建者条件")
        void departmentWithSelfScopeOrsCreator() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).orgUnitId(100L).tenantId(1L).build();
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student").effectiveScope(DataScope.DEPARTMENT)
                    .hasSelfScope(true).build();
            Object cond = build(merged, ctx);
            assertThat(sqlOf(cond))
                    .isEqualTo("(s.org_unit_id = ? OR s.created_by = ?)");
        }

        @Test
        @DisplayName("SELF scope → creator 等值条件")
        void selfScopeBuildsCreator() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).orgUnitId(100L).tenantId(1L).build();
            MergedDataScope merged = MergedDataScope.builder()
                    .moduleCode("student").effectiveScope(DataScope.SELF).build();
            Object cond = build(merged, ctx);
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
        }
    }

    // ==================================================================
    // intercept(Invocation) — early-return branches
    // ==================================================================
    @Nested
    @DisplayName("intercept — 早退分支与过滤注入")
    class Intercept {

        @Mock
        private Invocation invocation;
        @Mock
        private MappedStatement mappedStatement;

        /**
         * Builds a real {@link StatementHandler} (so the interceptor's cast succeeds)
         * exposing {@code delegate.mappedStatement} via getters — MyBatis SystemMetaObject
         * navigation reads {@code getDelegate().getMappedStatement()} reflectively.
         */
        private FakeStatementHandler handler(BoundSql boundSql) {
            return new FakeStatementHandler(new FakeDelegate(mappedStatement, boundSql), boundSql);
        }

        @Test
        @DisplayName("数据权限被禁用 → 直接 proceed, 不查 module")
        void disabledDataPermissionProceeds() throws Throwable {
            UserContextHolder.disableDataPermission();
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            verify(invocation).proceed();
            verify(dynamicModuleService, never()).getModuleConfig(anyLong(), anyString());
        }

        @Test
        @DisplayName("UserContext 为 null → 直接 proceed")
        void nullUserContextProceeds() throws Throwable {
            UserContextHolder.clear();
            UserContextHolder.enableDataPermission();
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            verify(invocation).proceed();
        }

        @Test
        @DisplayName("超级管理员 → 直接 proceed, 不注入过滤")
        void superAdminProceeds() throws Throwable {
            UserContextHolder.setContext(UserContext.builder()
                    .userId(1L).superAdmin(true).build());
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            verify(dynamicModuleService, never()).getModuleConfig(anyLong(), anyString());
        }

        @Test
        @DisplayName("INSERT 语句 → 跳过 (无 WHERE, 由应用层保证归属)")
        void insertStatementSkipped() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ALL, 0L, null))));
            when(invocation.getTarget()).thenReturn(handler(null));
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".insertOne");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            verify(dynamicModuleService, never()).getModuleConfig(anyLong(), anyString());
        }

        @Test
        @DisplayName("Mapper 无 @DataPermission 注解 → proceed, 不查 module")
        void noAnnotationProceeds() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ALL, 0L, null))));
            when(invocation.getTarget()).thenReturn(handler(null));
            when(mappedStatement.getId()).thenReturn(PlainMapper.class.getName() + ".selectList");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            verify(dynamicModuleService, never()).getModuleConfig(anyLong(), anyString());
        }

        @Test
        @DisplayName("module 配置不存在 → proceed, 不构建条件")
        void missingModuleConfigProceeds() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ALL, 0L, null))));
            when(invocation.getTarget()).thenReturn(handler(null));
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".selectList");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
            when(dynamicModuleService.getModuleConfig(anyLong(), anyString())).thenReturn(null);
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            verify(dataPermissionPolicyService, never())
                    .getScopeCodeForRole(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("module 被禁用 → proceed")
        void disabledModuleProceeds() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ALL, 0L, null))));
            when(invocation.getTarget()).thenReturn(handler(null));
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".selectList");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
            when(dynamicModuleService.getModuleConfig(anyLong(), anyString()))
                    .thenReturn(moduleConfig(false, ""));
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
        }

        @Test
        @DisplayName("ALL scope → 条件为 null, proceed 不改写 SQL")
        void allScopeNoFilterProceeds() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ALL, 0L, null))));
            FakeStatementHandler fake = handler(null);
            when(invocation.getTarget()).thenReturn(fake);
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".selectList");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
            when(dynamicModuleService.getModuleConfig(anyLong(), anyString()))
                    .thenReturn(moduleConfig(true, ""));
            when(dataPermissionPolicyService.getScopeCodeForRole(anyLong(), anyLong(), anyString()))
                    .thenReturn(DataScope.ALL.getCode());
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            // ALL scope short-circuits before getBoundSql() is consulted.
            assertThat(fake.boundSqlAccessed).isFalse();
        }

        @Test
        @DisplayName("DEPARTMENT scope → 改写 BoundSql 注入过滤并 proceed")
        void departmentScopeInjectsFilter() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(2L, ScopeType.ORG_UNIT, 200L, "1.10.200."))));
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".selectList");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.SELECT);
            Configuration configuration = new Configuration();
            when(mappedStatement.getConfiguration()).thenReturn(configuration);
            when(dynamicModuleService.getModuleConfig(anyLong(), anyString()))
                    .thenReturn(moduleConfig(true, ""));
            when(dataPermissionPolicyService.getScopeCodeForRole(anyLong(), anyLong(), anyString()))
                    .thenReturn(DataScope.DEPARTMENT.getCode());

            BoundSql realBoundSql = new BoundSql(
                    configuration, "SELECT s.id FROM user_student s WHERE s.deleted = 0",
                    new ArrayList<ParameterMapping>(), new Object());
            when(invocation.getTarget()).thenReturn(handler(realBoundSql));
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            assertThat(result).isEqualTo("ok");
            assertThat(realBoundSql.getSql()).contains("AND (s.org_unit_id = ?)");
            assertThat(realBoundSql.getParameterMappings()).hasSize(1);
            verify(invocation).proceed();
        }
    }

    /**
     * A real {@link StatementHandler} implementation exposing {@code delegate.mappedStatement}
     * via getters so the interceptor's MyBatis SystemMetaObject navigation resolves without a
     * full MyBatis routing handler. Only {@code getDelegate()} / {@code getBoundSql()} are
     * meaningful; the rest of the interface is unused by the interceptor.
     */
    @SuppressWarnings("unused")
    public static class FakeStatementHandler implements StatementHandler {
        private final FakeDelegate delegate;
        private final BoundSql boundSql;
        boolean boundSqlAccessed = false;

        FakeStatementHandler(FakeDelegate delegate, BoundSql boundSql) {
            this.delegate = delegate;
            this.boundSql = boundSql;
        }

        public FakeDelegate getDelegate() {
            return delegate;
        }

        @Override
        public BoundSql getBoundSql() {
            boundSqlAccessed = true;
            return boundSql;
        }

        @Override
        public java.sql.Statement prepare(java.sql.Connection connection, Integer transactionTimeout) {
            return null;
        }

        @Override
        public void parameterize(java.sql.Statement statement) {
        }

        @Override
        public void batch(java.sql.Statement statement) {
        }

        @Override
        public int update(java.sql.Statement statement) {
            return 0;
        }

        @Override
        public <E> List<E> query(java.sql.Statement statement,
                                 org.apache.ibatis.session.ResultHandler resultHandler) {
            return Collections.emptyList();
        }

        @Override
        public <E> org.apache.ibatis.cursor.Cursor<E> queryCursor(java.sql.Statement statement) {
            return null;
        }

        @Override
        public org.apache.ibatis.executor.parameter.ParameterHandler getParameterHandler() {
            return null;
        }
    }

    /**
     * Holds the {@code mappedStatement} and {@code boundSql} getters that the interceptor
     * navigates to. {@code delegate.mappedStatement} is read for the mapper id;
     * {@code delegate.boundSql.sql} is written back with the rewritten SQL.
     */
    @SuppressWarnings("unused")
    public static class FakeDelegate {
        private final MappedStatement mappedStatement;
        private final BoundSql boundSql;

        FakeDelegate(MappedStatement mappedStatement, BoundSql boundSql) {
            this.mappedStatement = mappedStatement;
            this.boundSql = boundSql;
        }

        public MappedStatement getMappedStatement() {
            return mappedStatement;
        }

        public BoundSql getBoundSql() {
            return boundSql;
        }
    }
}
