package com.school.management.domain.access.model.valueobject;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.SubjectScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScopeSpec 值对象单测 —— 验证三轴 (①org anchor / ②subject-relation / ③type) 便利谓词与 null 安全。
 */
class ScopeSpecTest {

    @Test
    void allAnchor_isUnbounded() {
        ScopeSpec s = ScopeSpec.builder().orgAnchor(OrgAnchor.ALL).applyTo("BOTH").build();
        assertThat(s.isOrgUnbounded()).isTrue();
        assertThat(s.hasTypeFilter()).isFalse();
    }

    @Test
    void relationAnchor_carriesParam() {
        ScopeSpec s = ScopeSpec.builder().orgAnchor(OrgAnchor.RELATION).anchorParam("admin").build();
        assertThat(s.getAnchorParam()).isEqualTo("admin");
        assertThat(s.isOrgUnbounded()).isFalse();
    }

    @Test
    void hasTypeFilter_trueWhenNonEmpty() {
        ScopeSpec s = ScopeSpec.builder().typeFilter(Set.of("STUDENT")).build();
        assertThat(s.hasTypeFilter()).isTrue();
    }

    @Test
    void hasTypeFilter_falseWhenNull() {
        ScopeSpec s = ScopeSpec.builder().typeFilter(null).build();
        assertThat(s.hasTypeFilter()).isFalse();
    }

    @Test
    void hasTypeFilter_falseWhenEmpty() {
        ScopeSpec s = ScopeSpec.builder().typeFilter(Set.of()).build();
        assertThat(s.hasTypeFilter()).isFalse();
    }

    @Test
    void hasRelInclude_trueWhenNonEmpty() {
        ScopeSpec s = ScopeSpec.builder().subjectRelInclude(Set.of("admin")).build();
        assertThat(s.hasRelInclude()).isTrue();
    }

    @Test
    void hasRelInclude_falseWhenNullOrEmpty() {
        assertThat(ScopeSpec.builder().subjectRelInclude(null).build().hasRelInclude()).isFalse();
        assertThat(ScopeSpec.builder().subjectRelInclude(Set.of()).build().hasRelInclude()).isFalse();
    }

    @Test
    void hasRelExclude_trueWhenNonEmpty() {
        ScopeSpec s = ScopeSpec.builder().subjectRelExclude(Set.of("guest")).build();
        assertThat(s.hasRelExclude()).isTrue();
    }

    @Test
    void hasRelExclude_falseWhenNullOrEmpty() {
        assertThat(ScopeSpec.builder().subjectRelExclude(null).build().hasRelExclude()).isFalse();
        assertThat(ScopeSpec.builder().subjectRelExclude(Set.of()).build().hasRelExclude()).isFalse();
    }

    // ── R3b grant-aware: 多 grant 须看全部 grant (任一命中), 非只首条 ──

    private static RelationGrant grant(SubjectScope s) {
        return new RelationGrant("owner_org", s, null, false, null);
    }

    @Test
    void isOrgUnbounded_multiGrant_anyAll_evenNonFirst() {
        assertThat(ScopeSpec.builder().relationGrants(List.of(grant(SubjectScope.ALL))).build()
                .isOrgUnbounded()).isTrue();
        // ALL 非首条 —— 修复点: 旧 deriveAxis 取首 grant 会漏判
        assertThat(ScopeSpec.builder()
                .relationGrants(List.of(grant(SubjectScope.SELF), grant(SubjectScope.ALL))).build()
                .isOrgUnbounded()).isTrue();
        assertThat(ScopeSpec.builder()
                .relationGrants(List.of(grant(SubjectScope.SELF), grant(SubjectScope.MY_ORG))).build()
                .isOrgUnbounded()).isFalse();
    }

    @Test
    void hasPluginDimGrant_multiGrant_anyPluginDim_evenNonFirst() {
        assertThat(ScopeSpec.builder().relationGrants(List.of(grant(SubjectScope.PLUGIN_DIM))).build()
                .hasPluginDimGrant()).isTrue();
        // PLUGIN_DIM 非首条 —— 否则 resourceType 注入漏 → plugin-dim resolve 失败
        assertThat(ScopeSpec.builder()
                .relationGrants(List.of(grant(SubjectScope.SELF), grant(SubjectScope.PLUGIN_DIM))).build()
                .hasPluginDimGrant()).isTrue();
        assertThat(ScopeSpec.builder()
                .relationGrants(List.of(grant(SubjectScope.SELF))).build()
                .hasPluginDimGrant()).isFalse();
    }

    @Test
    void grantAware_falseToLegacyAnchor_whenNoGrants() {
        // 无 relationGrants → 看 orgAnchor (旧 spec / sub-spec 行为不变)
        assertThat(ScopeSpec.builder().orgAnchor(OrgAnchor.PLUGIN_DIM).build().hasPluginDimGrant()).isTrue();
        assertThat(ScopeSpec.builder().orgAnchor(OrgAnchor.SELF).build().hasPluginDimGrant()).isFalse();
        assertThat(ScopeSpec.builder().orgAnchor(OrgAnchor.ALL).build().isOrgUnbounded()).isTrue();
    }
}
