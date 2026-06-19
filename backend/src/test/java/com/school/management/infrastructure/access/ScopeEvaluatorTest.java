package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.SubjectScope;
import com.school.management.domain.access.model.valueobject.RelationGrant;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import org.apache.ibatis.type.JdbcType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    private static final Long TENANT = 1L;

    private ScopeEvaluator evaluator() {
        return new ScopeEvaluator(pluginDataScopeRouter);
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
        return new ResourceScopeMeta("t", "org_unit_id", "created_by", "", false, "id", null);
    }

    /** membership 路径 meta (如 user): viaMembership, subjectCol=id。 */
    private ResourceScopeMeta membershipMeta() {
        return new ResourceScopeMeta("u", "org_unit_id", "created_by", "", true, "id", null);
    }

    private ResourceScopeMeta membershipMetaWithType() {
        return new ResourceScopeMeta("u", "org_unit_id", "created_by", "", true, "id", "user_type_code");
    }

    /** access_relation 路径 meta: resourceType="student", 非 membership, alias="t"。 */
    private ResourceScopeMeta accessRelationMeta() {
        return new ResourceScopeMeta("t", "org_unit_id", "created_by", "student", false, "id", null);
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

    // ---- 9. access_relation SELF: org-OR 不得被丢弃 (与 buildAccessRelationCondition 等价) ----
    @Test
    @DisplayName("access_relation SELF → 含 (subject=me) OR (subject_id IN org_units tree_path LIKE ?), org-OR 不丢")
    void accessRelationSelfKeepsOrgOr() {
        ScopeSpec spec = ScopeSpec.builder().orgAnchor(OrgAnchor.SELF).build();
        ScopeCondition c = evaluator().toSqlCondition(spec, accessRelationMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        // 基础 subject=me 单边
        assertThat(c.sql).contains("(ar.subject_type = 'user' AND ar.subject_id = ?)");
        // org-subject-OR 未被丢弃 (interceptor: orgPath != null 无条件 emit subtree 子查询)
        assertThat(c.sql).contains(
                "OR (ar.subject_type = 'org_unit' AND ar.subject_id IN ("
                + "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0))");
        // SELF 非 PRIMARY_ORG → 无 orgField 直过滤 OR
        assertThat(c.sql).doesNotContain("t.org_unit_id");
        // 参数: resType, tenantId, userId, tenantId2, orgPath
        assertThat(c.params).hasSize(5);
        assertThat(c.params.get(0).value).isEqualTo("student");
        assertThat(c.params.get(1).value).isEqualTo(TENANT);
        assertThat(c.params.get(2).value).isEqualTo(7L);
        assertThat(c.params.get(3).value).isEqualTo(TENANT);
        assertThat(c.params.get(4).value).isEqualTo("/1/100/%");
    }

    // ---- 10. access_relation PRIMARY_ORG no-subtree (DEPARTMENT): org-OR 仍 subtree, orgField 直过滤 = ? ----
    @Test
    @DisplayName("access_relation PRIMARY_ORG 无子树 → org-OR 用 tree_path 子查询(orgPath!=null), orgField 直过滤 = ?")
    void accessRelationPrimaryOrgNoSubtree() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.PRIMARY_ORG)
                .includeSubtree(false)
                .build();
        ScopeCondition c = evaluator().toSqlCondition(spec, accessRelationMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        // org-subject-OR keyed on orgPath != null (NOT subtree-gated) → 仍是 tree_path 子查询
        assertThat(c.sql).contains(
                "OR (ar.subject_type = 'org_unit' AND ar.subject_id IN ("
                + "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0))");
        // orgField 直过滤 OR (PRIMARY_ORG) — 无子树 → 单等值
        assertThat(c.sql).contains("OR t.org_unit_id = ?");
        assertThat(c.sql).doesNotContain("OR t.org_unit_id IN (SELECT id FROM org_units");
        // 参数: resType, tenantId, userId, tenantId2, orgPath, orgDirect
        assertThat(c.params).hasSize(6);
        ScopeCondition.Param last = c.params.get(c.params.size() - 1);
        assertThat(last.value).isEqualTo(100L);
    }

    // ---- 11. access_relation PRIMARY_ORG subtree: 两处皆 subtree ----
    @Test
    @DisplayName("access_relation PRIMARY_ORG + subtree → org-OR 与 orgField 直过滤皆用 tree_path 子查询")
    void accessRelationPrimaryOrgSubtree() {
        ScopeSpec spec = ScopeSpec.builder()
                .orgAnchor(OrgAnchor.PRIMARY_ORG)
                .includeSubtree(true)
                .build();
        ScopeCondition c = evaluator().toSqlCondition(spec, accessRelationMeta(), ctx(), 100L, "/1/100/", TENANT, 0);

        // org-subject-OR subtree
        assertThat(c.sql).contains(
                "OR (ar.subject_type = 'org_unit' AND ar.subject_id IN ("
                + "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0))");
        // orgField 直过滤 OR subtree
        assertThat(c.sql).contains(
                "OR t.org_unit_id IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)");
        assertThat(c.sql).doesNotContain("OR t.org_unit_id = ?");
        // 参数: resType, tenantId, userId, tenantId2, orgPath, tenantId3, orgPath2
        assertThat(c.params).hasSize(7);
        assertThat(c.params.get(5).value).isEqualTo(TENANT);
        assertThat(c.params.get(6).value).isEqualTo("/1/100/%");
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
}
