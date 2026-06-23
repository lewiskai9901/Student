package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.StorageKind;
import com.school.management.domain.access.model.SubjectScope;
import com.school.management.domain.access.model.valueobject.RelationGrant;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import org.apache.ibatis.type.JdbcType;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.school.management.infrastructure.extension.RecordRelationResolver;
import com.school.management.infrastructure.extension.ScopeContext;
import com.school.management.infrastructure.extension.SqlFragment;
import java.util.Map;

/**
 * Unit tests for {@link ScopeEvaluator#toSqlCondition}.
 *
 * <p>每个 case 断言 compose 出的 SQL 片段 + 参数序列 (位置绑定)。emitted SQL 必须与
 * 现 {@code DataPermissionInterceptor} 各 build* 方法等价 —— T7 瘦壳化的回归保证。
 *
 * <p>固定上下文: userId=7, orgUnitId=100, orgUnitPath="/1/100/", tenantId=1。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScopeEvaluator.toSqlCondition")
class ScopeEvaluatorTest {

    @Mock
    private PluginDataScopeRouter pluginDataScopeRouter;

    @Mock
    private ResourceRelationRegistry resourceRelationRegistry;

    @Mock
    private RecordRelationResolverRouter recordRelationResolverRouter;

    private static final Long TENANT = 1L;

    private ScopeEvaluator evaluator() {
        return new ScopeEvaluator(pluginDataScopeRouter, resourceRelationRegistry, recordRelationResolverRouter,
                new ChainCompiler(new ChainHopResolver(), resourceRelationRegistry));
    }

    private UserContext ctx() {
        return UserContext.builder()
                .userId(7L)
                .orgUnitId(100L)
                .orgUnitPath("/1/100/")
                .build();
    }

    /** org 字段路径 meta: 无 membership / 无 resourceType。 */
    private ResourceScopeMeta orgFieldMeta() {
        return new ResourceScopeMeta("t", "org_unit_id", "created_by", false, "id", null, null);
    }

    /** membership 路径 meta (如 user): viaMembership, subjectCol=id。 */
    private ResourceScopeMeta membershipMeta() {
        return new ResourceScopeMeta("u", "org_unit_id", "created_by", true, "id", null, null);
    }

    private ResourceScopeMeta membershipMetaWithType() {
        return new ResourceScopeMeta("u", "org_unit_id", "created_by", true, "id", "user_type_code", null);
    }


    // ---- 1. PRIMARY_ORG + subtree, org-field ----
    @Test
    @DisplayName("PRIMARY_ORG + subtree → tree_path 子查询, params [tenantId, path%]")
    void primaryOrgSubtree() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.PRIMARY_ORG)
                .includeSubtree(true)
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).contains(
                "t.org_unit_id IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)");
        assertThat(c.params).hasSize(2);
        assertThat(c.params.get(0).value).isEqualTo(TENANT);
        assertThat(c.params.get(0).jdbcType).isEqualTo(JdbcType.BIGINT);
        assertThat(c.params.get(1).value).isEqualTo("/1/100/%");
        assertThat(c.params.get(1).jdbcType).isEqualTo(JdbcType.VARCHAR);
    }

    @Test
    @DisplayName("PRIMARY_ORG 无子树 → 单 org 等值")
    void primaryOrgSingle() {
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.PRIMARY_ORG).includeSubtree(false).build();
        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("t.org_unit_id = ?");
        assertThat(c.params).hasSize(1);
        assertThat(c.params.get(0).value).isEqualTo(100L);
    }

    // ---- 2. RELATION anchorParam=admin, membership ----
    @Test
    @DisplayName("RELATION admin + membership → member 子查询 + relation = ? bound admin")
    void relationMembership() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.RELATION)
                .anchorParam("admin")
                .includeSubtree(false)
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).contains("u.id IN (SELECT ar.subject_id FROM access_relations ar");
        assertThat(c.sql).contains("ar.relation = 'member'");
        // anchorParam 替换硬编码 'admin' → 内层关系子查询用 mar.relation = ? 绑定
        assertThat(c.sql).contains("mar.relation = ?");
        boolean adminBound = c.params.stream().anyMatch(p -> "admin".equals(p.value));
        assertThat(adminBound).isTrue();
    }

    // ---- 3. RELATION admin + subjectRelExclude=[admin] ----
    @Test
    @DisplayName("RELATION admin + subjectRelExclude=[admin] → 附带 NOT IN (... relation IN (?) ...)")
    void relationExclude() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.RELATION)
                .anchorParam("admin")
                .subjectRelExclude(Set.of("admin"))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).contains("NOT IN (");
        assertThat(c.sql).contains("ar.relation IN (?)");
        long adminCount = c.params.stream().filter(p -> "admin".equals(p.value)).count();
        // 一次锚定关系 (mar.relation) + 一次 exclude relation
        assertThat(adminCount).isGreaterThanOrEqualTo(2);
    }

    // ---- 4. (3) + typeFilter=[STUDENT] ----
    @Test
    @DisplayName("RELATION admin + exclude + typeFilter=[STUDENT] → 末尾 AND u.user_type_code IN (?); STUDENT 最后")
    void relationExcludeWithType() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.RELATION)
                .anchorParam("admin")
                .subjectRelExclude(Set.of("admin"))
                .typeFilter(Set.of("STUDENT"))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMetaWithType(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).contains("u.user_type_code IN (?)");
        assertThat(c.sql).endsWith("u.user_type_code IN (?))");
        // type 参数排在最后
        ScopeCondition.Param last = c.params.get(c.params.size() - 1);
        assertThat(last.value).isEqualTo("STUDENT");
        assertThat(last.jdbcType).isEqualTo(JdbcType.VARCHAR);
    }

    // ---- 5. ALL + typeFilter=[STUDENT] → 仅类型 ----
    @Test
    @DisplayName("ALL + typeFilter=[STUDENT] → 仅 u.user_type_code IN (?)")
    void allWithTypeOnly() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.ALL)
                .typeFilter(Set.of("STUDENT"))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMetaWithType(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEqualTo("u.user_type_code IN (?)");
        assertThat(c.params).hasSize(1);
        assertThat(c.params.get(0).value).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("ALL 无类型过滤 → 空 SQL (放行)")
    void allUnbounded() {
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.ALL).build();
        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEmpty();
        assertThat(c.params).isEmpty();
    }

    // ---- 6. SELF org-field & membership ----
    @Test
    @DisplayName("SELF org-field → t.created_by = ?")
    void selfOrgField() {
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.SELF).build();
        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("t.created_by = ?");
        assertThat(c.params.get(0).value).isEqualTo(7L);
        assertThat(c.params.get(0).jdbcType).isEqualTo(JdbcType.BIGINT);
    }

    @Test
    @DisplayName("SELF membership → u.id = ?")
    void selfMembership() {
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.SELF).build();
        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("u.id = ?");
        assertThat(c.params.get(0).value).isEqualTo(7L);
    }

    // ---- 7. PLUGIN_DIM ----
    @Test
    @DisplayName("PLUGIN_DIM resolver=[5,6] → u.id IN (5,6)")
    void pluginDimNonEmpty() {
        when(pluginDataScopeRouter.resolve(eq("BY_MAJOR"), anyLong(), any()))
                .thenReturn(List.of(5L, 6L));
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("u.id IN (5,6)");
        assertThat(c.params).isEmpty();
    }

    @Test
    @DisplayName("PLUGIN_DIM resolver=null → 自降级 SELF (membership: subjectCol = ?)")
    void pluginDimNullDegrade() {
        when(pluginDataScopeRouter.resolve(eq("BY_MAJOR"), anyLong(), any())).thenReturn(null);
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("u.id = ?");
        assertThat(c.params.get(0).value).isEqualTo(7L);
    }

    @Test
    @DisplayName("PLUGIN_DIM resolver=null + org-field → 自降级 creator")
    void pluginDimNullDegradeCreator() {
        when(pluginDataScopeRouter.resolve(eq("BY_MAJOR"), anyLong(), any())).thenReturn(null);
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("t.created_by = ?");
        assertThat(c.params.get(0).value).isEqualTo(7L);
    }

    @Test
    @DisplayName("PLUGIN_DIM resolver=empty → 1 = 0")
    void pluginDimEmptyDeny() {
        when(pluginDataScopeRouter.resolve(eq("BY_MAJOR"), anyLong(), any())).thenReturn(List.of());
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.PLUGIN_DIM).anchorParam("BY_MAJOR").build();
        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).isEqualTo("1 = 0");
    }

    // ---- 8. CUSTOM_ORG subtree ----
    @Test
    @DisplayName("CUSTOM_ORG customOrgIds={9} subtree → tree_path 子查询")
    void customOrgSubtree() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.CUSTOM_ORG)
                .customOrgIds(Set.of(9L))
                .includeSubtree(true)
                .build();
        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(c.sql).contains("t.org_unit_id IN (");
        assertThat(c.sql).contains("SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE (");
        assertThat(c.sql).contains("SELECT CONCAT(tree_path, '%') FROM org_units WHERE id = ?))");
        // tenant + childOrg
        assertThat(c.params).hasSize(2);
        assertThat(c.params.get(0).value).isEqualTo(TENANT);
        assertThat(c.params.get(1).value).isEqualTo(9L);
    }

    // ---- 12. membership CUSTOM_ORG: 永远子树展开 JOIN 形态 (即使 includeSubtree=false) ----
    @Test
    @DisplayName("membership CUSTOM_ORG includeSubtree=false → 仍 emit always-expand JOIN (非裸 IN(9))")
    void membershipCustomOrgAlwaysExpand() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.CUSTOM_ORG)
                .customOrgIds(Set.of(9L))
                .includeSubtree(false)
                .build();
        ScopeCondition c = evaluator().toSqlCondition(spec, membershipMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        // 始终子树展开 JOIN 形态 (端口自 buildMembershipCondition CUSTOM)
        assertThat(c.sql).contains(
                "ar.resource_id IN (SELECT o.id FROM org_units o "
                + "JOIN org_units g ON g.id IN (9) "
                + "WHERE o.tenant_id = ? AND o.deleted = 0 "
                + "AND o.tree_path LIKE CONCAT(g.tree_path, '%'))");
        // 不得退化为裸 IN(9)
        assertThat(c.sql).doesNotContain("ar.resource_id IN (9)");
        // membership 外壳
        assertThat(c.sql).startsWith("u.id IN (SELECT ar.subject_id FROM access_relations ar");
        // 参数: _dp_memArTenant_ (外壳 tenant), _dp_memCustomTenant_ (JOIN tenant) — id 内联无参
        assertThat(c.params).hasSize(2);
        assertThat(c.params.get(0).value).isEqualTo(TENANT);
        assertThat(c.params.get(1).value).isEqualTo(TENANT);
    }

    // ---- 13. R3 多 grant OR: relation_grants 非空 → 各 grant 子条件 OR (多锚点) ----
    @Test
    @DisplayName("R3 多 grant: {creator,SELF} ∨ {owner_org,MY_ORG+subtree} → (created_by=? OR org_unit_id IN(子树))")
    void multiGrantOr() {
        ScopeSpec spec = ScopeSpec.builder()
                .relationGrants(List.of(
                        new RelationGrant("creator", SubjectScope.SELF, null, false, null),
                        new RelationGrant("owner_org", SubjectScope.MY_ORG, null, true, null)))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        // 两条 grant OR 叠加, 整体加括号
        assertThat(c.sql).startsWith("(").endsWith(")");
        assertThat(c.sql).contains("t.created_by = ?");
        assertThat(c.sql).contains(" OR ");
        assertThat(c.sql).contains(
                "t.org_unit_id IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)");
        // 参数顺序 (位置绑定): [creator userId=7, MY_ORG tenant=1, path=/1/100/%]
        assertThat(c.params).hasSize(3);
        assertThat(c.params.get(0).value).isEqualTo(7L);
        assertThat(c.params.get(1).value).isEqualTo(TENANT);
        assertThat(c.params.get(2).value).isEqualTo("/1/100/%");
    }

    // ---- P1: 多级关系链 grant (hops 非空) → ChainCompiler 路径 ----
    @Test
    @DisplayName("P1 多级链: hops=[我 admin 组织] + owner_org 终端 → org_unit_id IN (access_relations 子查询)")
    void hopChainGrant() {
        when(resourceRelationRegistry.relationOf("doc", "owner_org"))
                .thenReturn(Optional.of(new ResourceRelationRegistry.AnchorRow(
                        "owner_org", StorageKind.COLUMN, "org_unit_id", null)));
        ResourceScopeMeta meta = new ResourceScopeMeta(
                "t", "org_unit_id", "created_by", false, "id", null, "doc");
        ScopeSpec spec = ScopeSpec.builder()
                .relationGrants(List.of(new RelationGrant("owner_org", null, null, false, null,
                        List.of(new com.school.management.domain.access.model.chain.Hop(
                                List.of("admin"),
                                com.school.management.domain.access.model.chain.Combine.OR,
                                "org_unit", false)))))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, meta, ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).contains("t.org_unit_id IN (SELECT ar0.resource_id FROM access_relations ar0");
        assertThat(c.sql).contains("ar0.resource_type = 'org_unit'");
        assertThat(c.sql).doesNotContain(":");  // 命名参数已内联为位置 ?
        assertThat(c.params).anyMatch(p -> Long.valueOf(7L).equals(p.value)); // chmMe = userId 7
        assertThat(c.params).anyMatch(p -> "admin".equals(p.value));
    }

    @Test
    @DisplayName("R3 多 grant: 任一 grant unbounded(ALL) → 并集放行 (空 SQL)")
    void multiGrantUnboundedShortCircuits() {
        ScopeSpec spec = ScopeSpec.builder()
                .relationGrants(List.of(
                        new RelationGrant("creator", SubjectScope.SELF, null, false, null),
                        new RelationGrant("owner_org", SubjectScope.ALL, null, false, null)))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, orgFieldMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEmpty(); // ALL grant 使并集无界 → 放行
        assertThat(c.params).isEmpty();
    }

    // ---- 14. R4 RECORD_RELATION grant → record_relations 子查询 ----
    @Test
    @DisplayName("R4 RECORD_RELATION: reviewer (storage=RECORD_RELATION) → record_relations 子查询 (subject=SELF)")
    void recordRelationGrant_emitsSubquery() {
        when(resourceRelationRegistry.relationOf("inspection_record", "reviewer"))
                .thenReturn(Optional.of(new ResourceRelationRegistry.AnchorRow(
                        "reviewer", StorageKind.RECORD_RELATION, null, null)));
        ResourceScopeMeta meta = new ResourceScopeMeta(
                "t", "org_unit_id", "created_by", false, "id", null, "inspection_record");
        ScopeSpec spec = ScopeSpec.builder()
                .relationGrants(List.of(new RelationGrant("reviewer", SubjectScope.SELF, null, false, null)))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, meta, ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEqualTo(
                "t.id IN (SELECT record_id FROM record_relations WHERE resource_code = ? "
                + "AND relation_code = ? AND subject_type = 'USER' AND subject_id = ? AND deleted = 0 "
                + "AND (valid_to IS NULL OR valid_to > NOW()))");
        // 参数: [resourceCode, relationCode, userId]
        assertThat(c.params).hasSize(3);
        assertThat(c.params.get(0).value).isEqualTo("inspection_record");
        assertThat(c.params.get(1).value).isEqualTo("reviewer");
        assertThat(c.params.get(2).value).isEqualTo(7L);
    }

    @Test
    @DisplayName("R4 多 grant: creator(COLUMN) ∨ reviewer(RECORD_RELATION) → 列谓词 OR record_relations 子查询")
    void multiGrant_columnOrRecordRelation() {
        when(resourceRelationRegistry.relationOf("inspection_record", "reviewer"))
                .thenReturn(Optional.of(new ResourceRelationRegistry.AnchorRow(
                        "reviewer", StorageKind.RECORD_RELATION, null, null)));
        // creator 关系不是 RECORD_RELATION (registry mock 默认 empty) → 走列路径
        ResourceScopeMeta meta = new ResourceScopeMeta(
                "t", "org_unit_id", "created_by", false, "id", null, "inspection_record");
        ScopeSpec spec = ScopeSpec.builder()
                .relationGrants(List.of(
                        new RelationGrant("creator", SubjectScope.SELF, null, false, null),
                        new RelationGrant("reviewer", SubjectScope.SELF, null, false, null)))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, meta, ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).startsWith("(").endsWith(")");
        assertThat(c.sql).contains("t.created_by = ?");                 // creator COLUMN 路径
        assertThat(c.sql).contains(" OR ");
        assertThat(c.sql).contains("SELECT record_id FROM record_relations"); // reviewer RECORD_RELATION 路径
    }

    // ---- 15. R3b 正解 (R4): plugin-dim 用 resourceCode → 多 grant 混 PLUGIN_DIM 不污染非 PLUGIN_DIM grant ----
    @Test
    @DisplayName("R3b 正解: 多 grant PLUGIN_DIM ∨ MY_ORG (COLUMN) — plugin-dim 用 resourceCode, MY_ORG 走 orgField 不被污染")
    void multiGrant_pluginDimAndOrg_noResourceTypePollution() {
        when(pluginDataScopeRouter.resolve(eq("BY_CLASS"), anyLong(), eq("student")))
                .thenReturn(List.of(55L));
        // COLUMN 资源: resourceType="" (原生空, 不再被 interceptor 注入 moduleCode), resourceCode="student"
        ResourceScopeMeta meta = new ResourceScopeMeta(
                "t", "org_unit_id", "created_by", false, "id", null, "student");
        ScopeSpec spec = ScopeSpec.builder()
                .relationGrants(List.of(
                        new RelationGrant("owner_org", SubjectScope.PLUGIN_DIM, "BY_CLASS", false, null),
                        new RelationGrant("owner_org", SubjectScope.MY_ORG, null, true, null)))
                .build();

        ScopeCondition c = evaluator().toSqlCondition(spec, meta, ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).contains("t.id IN (55)");  // PLUGIN_DIM 用 resourceCode resolve (=student)
        assertThat(c.sql).contains(
                "t.org_unit_id IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)"); // MY_ORG orgField
        assertThat(c.sql).contains(" OR ");
        // 关键: MY_ORG 未被 resourceType 污染到 accessRelationSelect (R3b 污染正解)
        assertThat(c.sql).doesNotContain("access_relations");
    }

    // ---- 16. R3c PROVIDER: 接口式关系 → resolver 子查询 / id 集 / fail-closed ----

    private void stubProvider(String resourceCode, String relation, String bean) {
        when(resourceRelationRegistry.relationOf(resourceCode, relation))
                .thenReturn(Optional.of(new ResourceRelationRegistry.AnchorRow(
                        relation, StorageKind.PROVIDER, null, bean)));
    }

    private ResourceScopeMeta providerMeta() {
        return new ResourceScopeMeta("s", "org_unit_id", "created_by", false, "id", null, "user_student");
    }

    private ScopeSpec providerSpec() {
        return ScopeSpec.builder()
                .relationGrants(List.of(new RelationGrant("taught_by", SubjectScope.SELF, null, false, null)))
                .build();
    }

    @Test
    @DisplayName("R3c PROVIDER 子查询模式: resolver.subquery → s.id IN (<子查询>), 命名参数 :me 改写为 ? 绑当前用户")
    void provider_subqueryMode() {
        stubProvider("user_student", "taught_by", "teachingStudentResolver");
        when(recordRelationResolverRouter.resolve("teachingStudentResolver"))
                .thenReturn(Optional.of(new RecordRelationResolver() {
                    @Override public SqlFragment subquery(ScopeContext c) {
                        return SqlFragment.of(
                                "SELECT s2.id FROM user_student s2 JOIN teacher_assignments ta "
                                + "ON ta.org_unit_id = s2.org_unit_id WHERE ta.teacher_id = :me",
                                Map.of("me", c.userId()));
                    }
                }));

        ScopeCondition c = evaluator().toSqlCondition(providerSpec(), providerMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEqualTo(
                "s.id IN (SELECT s2.id FROM user_student s2 JOIN teacher_assignments ta "
                + "ON ta.org_unit_id = s2.org_unit_id WHERE ta.teacher_id = ?)");
        assertThat(c.params).hasSize(1);
        assertThat(c.params.get(0).value).isEqualTo(7L);          // ctx().userId
        assertThat(c.params.get(0).jdbcType).isEqualTo(JdbcType.BIGINT);
    }

    @Test
    @DisplayName("R3c PROVIDER id 集模式: subquery 返回 null → recordIds → s.id IN (?, ?)")
    void provider_idsMode() {
        stubProvider("user_student", "taught_by", "teachingStudentResolver");
        when(recordRelationResolverRouter.resolve("teachingStudentResolver"))
                .thenReturn(Optional.of(new RecordRelationResolver() {
                    @Override public List<Long> recordIds(ScopeContext c) { return List.of(301L, 302L); }
                }));

        ScopeCondition c = evaluator().toSqlCondition(providerSpec(), providerMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEqualTo("s.id IN (?, ?)");
        assertThat(c.params).hasSize(2);
        assertThat(c.params.get(0).value).isEqualTo(301L);
        assertThat(c.params.get(1).value).isEqualTo(302L);
    }

    @Test
    @DisplayName("R3c PROVIDER fail-closed: resolver bean 不可用 → 1 = 0 拒绝所有")
    void provider_failClosed_beanMissing() {
        stubProvider("user_student", "taught_by", "missingResolver");
        when(recordRelationResolverRouter.resolve("missingResolver")).thenReturn(Optional.empty());

        ScopeCondition c = evaluator().toSqlCondition(providerSpec(), providerMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEqualTo("1 = 0");
        assertThat(c.params).isEmpty();
    }

    @Test
    @DisplayName("R3c PROVIDER fail-closed: subquery 与 recordIds 皆空 → 1 = 0")
    void provider_failClosed_bothNull() {
        stubProvider("user_student", "taught_by", "emptyResolver");
        when(recordRelationResolverRouter.resolve("emptyResolver"))
                .thenReturn(Optional.of(new RecordRelationResolver() { }));  // 全用默认 (都 null)

        ScopeCondition c = evaluator().toSqlCondition(providerSpec(), providerMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        assertThat(c.sql).isEqualTo("1 = 0");
    }
}
