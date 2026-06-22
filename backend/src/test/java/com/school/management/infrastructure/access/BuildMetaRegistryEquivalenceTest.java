package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R2.2 字节等价证明 (buildMeta 改读 resource_relations 注册表后)。
 *
 * <p>注册表只驱动 {@code orgUnitField / creatorField / viaMembership} 三字段; 对有列锚的普通资源,
 * 注册表派生值 = R2.1 冻结的有效值 = legacy 值 (逐字节相同, 无需测)。<b>唯一分歧</b>在<b>成员主体</b>
 * (user/student): legacy 的 creatorField 是 data_resources.created_by, 注册表无 creator 行 → coerce 成 ""。
 *
 * <p>本测证明: 成员路径 ({@code viaMembership=true}) 的 SQL <b>不消费</b> orgUnitField/creatorField
 * (membership 用 ar.resource_id 锚 org、用 subjectCol 锚 self), 故该差异对产出 SQL/参数<b>零影响</b>。
 * 即注册表化对成员主体也字节等价。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("R2.2 buildMeta 注册表化字节等价 (成员主体路径)")
class BuildMetaRegistryEquivalenceTest {

    @Mock
    private PluginDataScopeRouter router;

    @Mock
    private ResourceRelationRegistry resourceRelationRegistry;

    private static final Long TENANT = 1L;

    private ScopeEvaluator evaluator() {
        return new ScopeEvaluator(router, resourceRelationRegistry, org.mockito.Mockito.mock(RecordRelationResolverRouter.class));
    }

    private UserContext ctx() {
        return UserContext.builder().userId(7L).orgUnitId(100L).orgUnitPath("/1/100/").build();
    }

    /** legacy: 成员主体 creatorField=created_by (来自 data_resources)。 */
    private ResourceScopeMeta legacyMembership() {
        return new ResourceScopeMeta("u", "", "created_by", "", true, "id", null, null);
    }

    /** registry: 注册表无 creator 行 → coerce 成 "" (orgUnitField 同为 "")。其余字段不变。 */
    private ResourceScopeMeta registryMembership() {
        return new ResourceScopeMeta("u", "", "", "", true, "id", null, null);
    }

    private void assertSameSql(ScopeSpec spec) {
        ScopeCondition legacy = evaluator().toSqlCondition(spec, legacyMembership(), ctx(), 100L, "/1/100/", TENANT, 0);
        ScopeCondition registry = evaluator().toSqlCondition(spec, registryMembership(), ctx(), 100L, "/1/100/", TENANT, 0);
        assertThat(registry.sql).isEqualTo(legacy.sql);
        assertThat(registry.params).hasSameSizeAs(legacy.params);
    }

    @Test
    @DisplayName("SELF → 同 SQL (membership self 用 subjectCol, 不读 creatorField)")
    void self() {
        assertSameSql(ScopeSpec.builder().orgAnchor(OrgAnchor.SELF).build());
    }

    @Test
    @DisplayName("PRIMARY_ORG + 子树 → 同 SQL")
    void primaryOrgSubtree() {
        assertSameSql(ScopeSpec.builder().orgAnchor(OrgAnchor.PRIMARY_ORG).includeSubtree(true).build());
    }

    @Test
    @DisplayName("PRIMARY_ORG 无子树 → 同 SQL")
    void primaryOrgSingle() {
        assertSameSql(ScopeSpec.builder().orgAnchor(OrgAnchor.PRIMARY_ORG).includeSubtree(false).build());
    }

    @Test
    @DisplayName("ALL → 同 SQL (空, 放行)")
    void all() {
        assertSameSql(ScopeSpec.builder().orgAnchor(OrgAnchor.ALL).build());
    }

    @Test
    @DisplayName("RELATION admin → 同 SQL")
    void relation() {
        assertSameSql(ScopeSpec.builder().orgAnchor(OrgAnchor.RELATION).anchorParam("admin").build());
    }

    @Test
    @DisplayName("RELATION admin + subjectRelExclude → 同 SQL")
    void relationExclude() {
        assertSameSql(ScopeSpec.builder()
                .orgAnchor(OrgAnchor.RELATION).anchorParam("admin")
                .subjectRelExclude(Set.of("admin")).build());
    }

    @Test
    @DisplayName("CUSTOM_ORG → 同 SQL (membership 始终子树展开 JOIN)")
    void customOrg() {
        assertSameSql(ScopeSpec.builder()
                .orgAnchor(OrgAnchor.CUSTOM_ORG).customOrgIds(Set.of(9L)).includeSubtree(false).build());
    }

    // ── 审计 P1: 非成员路径与成员路径相反, SELF 会消费 creatorField → 注册表必须登 creator ──
    @Test
    @DisplayName("非成员 SELF 消费 creatorField(与成员路径相反): 登 created_by → 't.created_by=?', 不登('') → 't.=?' 发散")
    void orgFieldSelfConsumesCreatorField() {
        ScopeSpec self = ScopeSpec.builder().orgAnchor(OrgAnchor.SELF).build();
        ResourceScopeMeta withCreator = new ResourceScopeMeta("t", "org_unit_id", "created_by", "", false, "id", null, null);
        ResourceScopeMeta noCreator   = new ResourceScopeMeta("t", "org_unit_id", "",           "", false, "id", null, null);
        String a = evaluator().toSqlCondition(self, withCreator, ctx(), 100L, "/1/100/", TENANT, 0).sql;
        String b = evaluator().toSqlCondition(self, noCreator,   ctx(), 100L, "/1/100/", TENANT, 0).sql;
        // 证明 creatorField 在非成员 SELF 确实进 SQL —— 这就是 student_grade/school_class 必须登 creator 的原因
        assertThat(a).isEqualTo("t.created_by = ?");
        assertThat(b).isEqualTo("t. = ?");
        assertThat(a).isNotEqualTo(b);
    }
}
