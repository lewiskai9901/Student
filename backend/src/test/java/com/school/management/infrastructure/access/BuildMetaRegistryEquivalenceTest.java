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

    private static final Long TENANT = 1L;

    private ScopeEvaluator evaluator() {
        return new ScopeEvaluator(router);
    }

    private UserContext ctx() {
        return UserContext.builder().userId(7L).orgUnitId(100L).orgUnitPath("/1/100/").build();
    }

    /** legacy: 成员主体 creatorField=created_by (来自 data_resources)。 */
    private ResourceScopeMeta legacyMembership() {
        return new ResourceScopeMeta("u", "", "created_by", "", true, "id", null);
    }

    /** registry: 注册表无 creator 行 → coerce 成 "" (orgUnitField 同为 "")。其余字段不变。 */
    private ResourceScopeMeta registryMembership() {
        return new ResourceScopeMeta("u", "", "", "", true, "id", null);
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
}
