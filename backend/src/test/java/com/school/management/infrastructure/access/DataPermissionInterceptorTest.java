package com.school.management.infrastructure.access;

import com.school.management.application.access.DynamicModuleService;
import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
 *   <li><b>T7 委托行为</b> ({@code buildScopedCondition}): ALL 短路→null / 多角色 OR 合并 /
 *       无配置→SELF / 全 empty→1=0 / 委托 {@link ScopeEvaluator} 各资源路径。SQL 片段的逐字节
 *       等价由 {@code ScopeEvaluatorTest} 保证, 此处只验证拦截器自有的编排逻辑。</li>
 *   <li>The {@code intercept(Invocation)} early-return branches.</li>
 * </ul>
 *
 * <p><b>T7 注</b>: 旧 {@code buildSingleRoleCondition / buildMembershipCondition /
 * buildAccessRelationCondition / buildPluginDimCondition / buildCustomCondition /
 * buildLegacyOrgFilterCondition} 已删除并迁移到 {@link ScopeEvaluator} (compose 单一真相源),
 * 它们的精确 SQL 断言现位于 {@code ScopeEvaluatorTest}。本测试用<b>真实</b> {@link ScopeEvaluator}
 * (router 用 mock) 跑委托路径, 用 mock {@code getScopeSpec} 喂规格。
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

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private DataPermissionInterceptor interceptor;
    private ScopeEvaluator scopeEvaluator;
    private ResourceRelationRegistry resourceRelationRegistry;

    @BeforeEach
    void setUp() {
        interceptor = new DataPermissionInterceptor();
        ReflectionTestUtils.setField(interceptor, "dynamicModuleService", dynamicModuleService);
        ReflectionTestUtils.setField(interceptor, "dataPermissionPolicyService", dataPermissionPolicyService);
        ReflectionTestUtils.setField(interceptor, "jdbcTemplate", jdbcTemplate);
        // Tier 1: buildMeta 锚点改由 resourceRelationRegistry 驱动 (注解锚兜底已删)。
        // 桩 forResource → 各 module 的 DerivedAnchor: student=列锚, user=成员图。
        // (字段, 便于个别测试覆写为 empty 验证 fail-fast)
        resourceRelationRegistry = mock(ResourceRelationRegistry.class);
        when(resourceRelationRegistry.forResource(anyString()))
                .thenReturn(Optional.of(new ResourceRelationRegistry.DerivedAnchor(false, "org_unit_id", "created_by")));
        when(resourceRelationRegistry.forResource("user"))
                .thenReturn(Optional.of(new ResourceRelationRegistry.DerivedAnchor(true, null, null)));
        ReflectionTestUtils.setField(interceptor, "resourceRelationRegistry", resourceRelationRegistry);
        // R4: ScopeEvaluator 持 router + registry (registry 供 RECORD_RELATION 检测)。
        scopeEvaluator = new ScopeEvaluator(pluginDataScopeRouter, resourceRelationRegistry, org.mockito.Mockito.mock(RecordRelationResolverRouter.class));
        ReflectionTestUtils.setField(interceptor, "scopeEvaluator", scopeEvaluator);
        UserContextHolder.clear();
        UserContextHolder.enableDataPermission();
    }

    // ── ScopeSpec 工厂 (T7): 用 OrgAnchor 直接构造规格, 替代旧 DataScope 码 ──
    private ScopeSpec specOf(OrgAnchor anchor, boolean subtree) {
        return ScopeSpec.builder().orgAnchor(anchor).includeSubtree(subtree).build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // Test fixtures: mapper classes / annotations
    // ------------------------------------------------------------------

    @DataPermission(module = "student", tableAlias = "s")
    interface AnnotatedMapper {
        List<Object> selectList();

        @DataPermission(module = "method_module", tableAlias = "m")
        List<Object> selectWithMethodAnnotation();
    }

    interface PlainMapper {
        List<Object> selectList();
    }

    /** 方法级 @DataPermission only (无接口级 + insert 方法无注解) — 模拟方法级 plugin mapper。 */
    interface MethodOnlyMapper {
        @DataPermission(module = "method_only_res")
        List<Object> findByX();
        Object insert(Object entity);   // 无注解 (同 BaseMapper.insert)
    }

    private DataPermission stubAnnotation() {
        return AnnotatedMapper.class.getAnnotation(DataPermission.class);
    }

    private DataModulePO moduleConfig(boolean enabled, String resourceType) {
        DataModulePO po = new DataModulePO();
        po.setModuleCode("student");
        po.setEnabled(enabled);
        po.setResourceType(resourceType);
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
        @DisplayName("R8 写鉴权: UPDATE ... WHERE → 注入 AND (scope), 写范围真拦截")
        void writeScope_appendsToUpdate() {
            // UPDATE/DELETE 走 WRITE actionClass, 同样经 injectFilterCondition (通用 WHERE 注入)。
            // 注入后 UPDATE 仅命中 scope 内的行 → 范围外行 0 rows affected (静默拦截)。
            String out = inject("UPDATE insp_submissions SET status = ? WHERE id = ?", "org_unit_id IN (?)");
            assertThat(out).isEqualTo(
                    "UPDATE insp_submissions SET status = ? WHERE id = ? AND (org_unit_id IN (?))");
        }

        @Test
        @DisplayName("R8 写鉴权: DELETE ... WHERE → 注入 AND (scope)")
        void writeScope_appendsToDelete() {
            String out = inject("DELETE FROM insp_submissions WHERE id = ?", "org_unit_id IN (?)");
            assertThat(out).isEqualTo(
                    "DELETE FROM insp_submissions WHERE id = ? AND (org_unit_id IN (?))");
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
    // buildScopedCondition — T7 委托 ScopeEvaluator 的编排逻辑
    // ==================================================================
    @Nested
    @DisplayName("buildScopedCondition — 委托 ScopeEvaluator + 多角色编排")
    class BuildScopedCondition {

        private ScopeCondition build(DataPermission ann, DataModulePO module, UserContext ctx, Long tenantId) {
            return ReflectionTestUtils.invokeMethod(interceptor, "buildScopedCondition",
                    ann, module, ctx, tenantId, "READ");
        }

        private String sqlOf(ScopeCondition cond) {
            return cond.sql;
        }

        @Test
        @DisplayName("Tier 1 fail-fast: 模块在注册表查无锚 (forResource empty) → 抛 IllegalStateException, 不再注解兜底")
        void unregisteredModuleFailsFast() {
            // 覆写: student 模块在注册表查无锚 (模拟 contribution 漏登/写失败); 删兜底后无后备
            // → buildMeta fail-fast (在 per-role 循环前抛, 故无需 getScopeSpec 桩)
            when(resourceRelationRegistry.forResource("student")).thenReturn(Optional.empty());
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(1L, ScopeType.ALL, 0L, null)));

            assertThatThrownBy(() -> build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("student");
        }

        @Test
        @DisplayName("ALL scopeType + ALL anchor (无类型/关系过滤) → 短路返回 null (不过滤)")
        void allScopeShortCircuits() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(1L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(1L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.ALL, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNull();
        }

        @Test
        @DisplayName("DEPARTMENT (PRIMARY_ORG 无子树, ORG_UNIT scope) → '别名.org字段 = ?' 用角色 scope org")
        void departmentScopeBuildsEqualsCondition() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(2L, ScopeType.ORG_UNIT, 200L, "1.10.200.")));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(2L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.org_unit_id = ?");
            assertThat(cond.params).hasSize(1);
            // per-role effective org: ORG_UNIT scope → 角色 scope id 200 (非用户主组织 100)
            assertThat(cond.params.get(0).value).isEqualTo(200L);
        }

        @Test
        @DisplayName("DEPARTMENT_AND_BELOW (PRIMARY_ORG + 子树) → tree_path LIKE 子查询")
        void departmentAndBelowBuildsTreePathSubquery() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(3L, ScopeType.ORG_UNIT, 300L, "1.10.300.")));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(3L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, true));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).contains("org_units").contains("tree_path LIKE ?");
            assertThat(cond.params).hasSize(2);
            // 角色 scope path 1.10.300. (非用户主组织 path)
            assertThat(cond.params.get(1).value).isEqualTo("1.10.300.%");
        }

        @Test
        @DisplayName("SELF anchor → 'creator 字段 = ?' 当前用户")
        void selfScopeBuildsCreatorCondition() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(4L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(4L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.SELF, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
            assertThat(cond.params).hasSize(1);
        }

        @Test
        @DisplayName("角色无配置 (getScopeSpec 返回 null) → 默认降级 SELF")
        void unconfiguredRoleDefaultsToSelf() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(5L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(5L), anyString(), anyString()))
                    .thenReturn(null);

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
        }

        @Test
        @DisplayName("CUSTOM_ORG → 委托 evaluator emit tree_path 子查询 (subtree)")
        void customScopeDelegatesToEvaluator() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(7L, ScopeType.ALL, 0L, null)));
            ScopeSpec spec = ScopeSpec.builder()
                    .orgAnchor(OrgAnchor.CUSTOM_ORG)
                    .customOrgIds(new HashSet<>(Set.of(901L)))
                    .includeSubtree(true)
                    .build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(7L), anyString(), anyString()))
                    .thenReturn(spec);

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).contains("org_unit_id IN").contains("tree_path LIKE");
        }

        @Test
        @DisplayName("插件维度 scope (PLUGIN_DIM) → 委托 router, id IN (...); resourceType 兜底 moduleCode")
        void pluginDimScopeBuildsInClause() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(8L, ScopeType.ALL, 0L, null)));
            ScopeSpec spec = ScopeSpec.builder()
                    .orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(8L), anyString(), anyString()))
                    .thenReturn(spec);
            // moduleConfig resourceType="" → 兜底 moduleCode "student" 作为 resolve 资源类型
            when(pluginDataScopeRouter.resolve(eq("BY_MAJOR"), anyLong(), eq("student")))
                    .thenReturn(List.of(11L, 22L, 33L));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.id IN (11,22,33)");
        }

        @Test
        @DisplayName("插件维度 resolver 返回 null → 安全降级 SELF (creator 过滤)")
        void pluginDimNullResolverDegradesToSelf() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(9L, ScopeType.ALL, 0L, null)));
            ScopeSpec spec = ScopeSpec.builder()
                    .orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(9L), anyString(), anyString()))
                    .thenReturn(spec);
            when(pluginDataScopeRouter.resolve(anyString(), anyLong(), anyString())).thenReturn(null);

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.created_by = ?");
        }

        @Test
        @DisplayName("插件维度 resolver 返回空列表 → 拒绝所有 '1 = 0'")
        void pluginDimEmptyResolverDeniesAll() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(10L, ScopeType.ALL, 0L, null)));
            ScopeSpec spec = ScopeSpec.builder()
                    .orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(10L), anyString(), anyString()))
                    .thenReturn(spec);
            when(pluginDataScopeRouter.resolve(anyString(), anyLong(), anyString()))
                    .thenReturn(Collections.emptyList());

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, "student"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("1 = 0");
        }

        @Test
        @DisplayName("viaMembership + PRIMARY_ORG → 委托 evaluator 走 member 关系子查询")
        void membershipDelegatesToEvaluator() {
            UserContext ctx = userWithScopedRoles(List.of(scopedRole(12L, ScopeType.ORG_UNIT, 200L, "1.10.200.")));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(12L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, false));

            ScopeCondition cond = build(membershipAnnotation(), moduleConfig(true, "user"), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond))
                    .startsWith("u.id IN (")
                    .contains("ar.relation = 'member'")
                    .contains("ar.resource_id = ?");
        }

        @Test
        @DisplayName("多角色 → 各自条件 OR 组合, 参数序拼接")
        void multipleRolesAreOrCombined() {
            UserContext ctx = userWithScopedRoles(List.of(
                    scopedRole(20L, ScopeType.ORG_UNIT, 500L, "1.10.500."),
                    scopedRole(21L, ScopeType.ALL, 0L, null)));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(20L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, false));
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(21L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.SELF, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).startsWith("(").contains(" OR ").endsWith(")");
            assertThat(cond.params).hasSize(2);
        }

        @Test
        @DisplayName("无有效条件 (org 解析失败的退化空) → 拒绝所有 '1 = 0'")
        void noValidConditionDeniesAll() {
            // PRIMARY_ORG 但用户无 orgId/orgPath → evaluator 产空 cond (predicate==null) → skip → deny
            UserContext noOrgCtx = UserContext.builder()
                    .userId(42L).orgUnitId(null).orgUnitPath(null)
                    .tenantId(1L)
                    .scopedRoles(List.of(scopedRole(30L, ScopeType.ALL, 0L, null)))
                    .build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(30L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), noOrgCtx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("1 = 0");
        }

        @Test
        @DisplayName("无 scopedRoles → 走 legacy 路径 (按 roleIds 委托 evaluator)")
        void emptyScopedRolesFallsBackToLegacy() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).orgUnitId(100L).orgUnitPath("1.10.100.")
                    .roleIds(List.of(99L))
                    .tenantId(1L)
                    .scopedRoles(Collections.emptyList())
                    .build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(99L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNotNull();
            assertThat(sqlOf(cond)).isEqualTo("s.org_unit_id = ?");
            // legacy 锚点 = 用户主组织 100
            assertThat(cond.params.get(0).value).isEqualTo(100L);
        }

        @Test
        @DisplayName("legacy 路径 ALL anchor → 返回 null (不过滤)")
        void legacyAllScopeReturnsNull() {
            UserContext ctx = UserContext.builder()
                    .userId(42L).orgUnitId(100L).roleIds(List.of(99L)).tenantId(1L)
                    .scopedRoles(null)
                    .build();
            when(dataPermissionPolicyService.getScopeSpec(eq(1L), eq(99L), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.ALL, false));

            ScopeCondition cond = build(stubAnnotation(), moduleConfig(true, ""), ctx, 1L);
            assertThat(cond).isNull();
        }

        // membership 注解 (供 membershipDelegatesToEvaluator 用)
        @DataPermission(module = "user", tableAlias = "u")
        interface MembershipMapper {
            List<Object> selectList();
        }

        private DataPermission membershipAnnotation() {
            return MembershipMapper.class.getAnnotation(DataPermission.class);
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
        @DisplayName("INSERT 语句 → 走 P3-INSERT 授权 (不再早退); moduleConfig 缺失 → fail-safe 放行")
        void insertStatementSkipped() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ALL, 0L, null))));
            when(invocation.getTarget()).thenReturn(handler(null));
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".insertOne");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);
            when(invocation.proceed()).thenReturn("ok");

            Object result = interceptor.intercept(invocation);

            // R8 P3-INSERT: INSERT 不再早退, 进入 annotation/moduleConfig 解析; moduleConfig 未 mock=null
            // → moduleConfig==null 早退放行 (无 SQL 注入)。getModuleConfig 现会被调用一次 (行为变更)。
            assertThat(result).isEqualTo("ok");
            verify(dynamicModuleService).getModuleConfig(anyLong(), anyString());
        }

        // ── R8 P3-INSERT 授权 deny/allow 逻辑 (org-bounded grant 探针 → 越界拒绝) ──

        private com.school.management.domain.access.model.valueobject.ScopeSpec grantSpec(
                String relation, com.school.management.domain.access.model.SubjectScope subject) {
            return com.school.management.domain.access.model.valueobject.ScopeSpec.builder()
                    .relationGrants(List.of(new com.school.management.domain.access.model.valueobject.RelationGrant(
                            relation, subject, null, false, null)))
                    .build();
        }

        private BoundSql insertBoundSql(Object ownerOrg) {
            java.util.Map<String, Object> entity = new java.util.HashMap<>();
            entity.put("orgUnitId", ownerOrg);
            return new BoundSql(new Configuration(), "INSERT INTO student (org_unit_id) VALUES (?)",
                    new ArrayList<ParameterMapping>(), entity);
        }

        private void primeInsert(com.school.management.domain.access.model.valueobject.ScopeSpec spec) {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ORG_UNIT, 100L, "1.10.100."))));
            when(mappedStatement.getId()).thenReturn(AnnotatedMapper.class.getName() + ".insert");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);
            when(dynamicModuleService.getModuleConfig(anyLong(), anyString())).thenReturn(moduleConfig(true, ""));
            when(dataPermissionPolicyService.getScopeSpec(anyLong(), anyLong(), anyString(), eq("WRITE")))
                    .thenReturn(spec);
            // R8 opt-in: 该资源已标注 INSERT 授权 (否则 gate 早退放行)
            when(resourceRelationRegistry.isInsertGuarded(anyString())).thenReturn(true);
        }

        @Test
        @DisplayName("R8 P3-INSERT: owner_org 越界 (org-bounded grant, 探针 false) → AccessDeniedException")
        void insertDeniedWhenOrgOutOfScope() throws Throwable {
            primeInsert(grantSpec("owner_org", com.school.management.domain.access.model.SubjectScope.MY_ORG));
            when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Boolean.class)))
                    .thenReturn(false);   // owner_org 不在可写组织集
            when(invocation.getTarget()).thenReturn(handler(insertBoundSql(999L)));

            assertThatThrownBy(() -> interceptor.intercept(invocation))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
            verify(invocation, never()).proceed();   // 拒绝 → 不执行 INSERT
        }

        @Test
        @DisplayName("R8 P3-INSERT: owner_org 在范围内 (探针 true) → 放行 proceed")
        void insertAllowedWhenOrgInScope() throws Throwable {
            primeInsert(grantSpec("owner_org", com.school.management.domain.access.model.SubjectScope.MY_ORG));
            when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Boolean.class)))
                    .thenReturn(true);
            when(invocation.getTarget()).thenReturn(handler(insertBoundSql(100L)));
            when(invocation.proceed()).thenReturn("ok");

            assertThat(interceptor.intercept(invocation)).isEqualTo("ok");
        }

        @Test
        @DisplayName("R8 P3-INSERT: 方法级 mapper insert (无注解) → any-method 解析模块 → 仍受授权 (deny)")
        void insertMethodLevelMapper_resolvedViaAnyMethod() throws Throwable {
            UserContextHolder.setContext(userWithScopedRoles(
                    List.of(scopedRole(1L, ScopeType.ORG_UNIT, 100L, "1.10.100."))));
            // insert 方法本身无 @DataPermission → 走 resolveAnyMethodAnnotation 扫到 findByX 的 module
            when(mappedStatement.getId()).thenReturn(MethodOnlyMapper.class.getName() + ".insert");
            when(mappedStatement.getSqlCommandType()).thenReturn(SqlCommandType.INSERT);
            when(dynamicModuleService.getModuleConfig(anyLong(), anyString())).thenReturn(moduleConfig(true, ""));
            when(dataPermissionPolicyService.getScopeSpec(anyLong(), anyLong(), anyString(), eq("WRITE")))
                    .thenReturn(grantSpec("owner_org", com.school.management.domain.access.model.SubjectScope.MY_ORG));
            when(resourceRelationRegistry.isInsertGuarded(anyString())).thenReturn(true);
            when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Boolean.class))).thenReturn(false);
            when(invocation.getTarget()).thenReturn(handler(insertBoundSql(999L)));

            assertThatThrownBy(() -> interceptor.intercept(invocation))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        @DisplayName("R8 P3-INSERT: creator grant → 新行我即创建者, 放行 (不探针)")
        void insertAllowedForCreatorGrant() throws Throwable {
            primeInsert(grantSpec("creator", com.school.management.domain.access.model.SubjectScope.SELF));
            when(invocation.getTarget()).thenReturn(handler(insertBoundSql(999L)));
            when(invocation.proceed()).thenReturn("ok");

            assertThat(interceptor.intercept(invocation)).isEqualTo("ok");
            verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Boolean.class), (Object[]) any());
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
                    .getScopeSpec(anyLong(), anyLong(), anyString(), anyString());
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
            when(dataPermissionPolicyService.getScopeSpec(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.ALL, false));
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
            when(dataPermissionPolicyService.getScopeSpec(anyLong(), anyLong(), anyString(), anyString()))
                    .thenReturn(specOf(OrgAnchor.PRIMARY_ORG, false));

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
