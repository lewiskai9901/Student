package com.school.management.domain.access.model.valueobject;

import com.school.management.domain.access.model.OrgAnchor;
import org.junit.jupiter.api.Test;

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
}
