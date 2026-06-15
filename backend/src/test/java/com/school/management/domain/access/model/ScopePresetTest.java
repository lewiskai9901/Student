package com.school.management.domain.access.model;

import com.school.management.domain.access.model.valueobject.ScopeSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScopePreset} —— named-scope catalog (UI sugar over axis ①)
 * and legacy {@code role_data_scopes.scope_type} translation.
 */
class ScopePresetTest {

    @Test
    void managedOrgs_mapsToRelationAdmin() {
        ScopeSpec s = ScopePreset.MANAGED_ORGS.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.RELATION);
        assertThat(s.getAnchorParam()).isEqualTo("admin");
        assertThat(s.isIncludeSubtree()).isFalse();
    }

    @Test
    void managedOrgsAndBelow_mapsToRelationAdminSubtree() {
        ScopeSpec s = ScopePreset.MANAGED_ORGS_AND_BELOW.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.RELATION);
        assertThat(s.getAnchorParam()).isEqualTo("admin");
        assertThat(s.isIncludeSubtree()).isTrue();
    }

    @Test
    void deptAndBelow_mapsToPrimaryOrgSubtree() {
        ScopeSpec s = ScopePreset.DEPARTMENT_AND_BELOW.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.PRIMARY_ORG);
        assertThat(s.getAnchorParam()).isNull();
        assertThat(s.isIncludeSubtree()).isTrue();
    }

    @Test
    void dept_mapsToPrimaryOrgNoSubtree() {
        ScopeSpec s = ScopePreset.DEPARTMENT.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.PRIMARY_ORG);
        assertThat(s.isIncludeSubtree()).isFalse();
    }

    @Test
    void all_mapsToAll() {
        ScopeSpec s = ScopePreset.ALL.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.ALL);
        assertThat(s.getAnchorParam()).isNull();
        assertThat(s.isIncludeSubtree()).isFalse();
    }

    @Test
    void self_mapsToSelf() {
        ScopeSpec s = ScopePreset.SELF.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.SELF);
    }

    @Test
    void custom_mapsToCustomOrg() {
        ScopeSpec s = ScopePreset.CUSTOM.toSpec();
        assertThat(s.getOrgAnchor()).isEqualTo(OrgAnchor.CUSTOM_ORG);
        assertThat(s.isIncludeSubtree()).isFalse();
    }

    /** Mapping table §2: each preset carries the exact displayName + level. */
    @Test
    void displayNamesAndLevels_matchDesignTable() {
        assertThat(ScopePreset.ALL.getDisplayName()).isEqualTo("全部数据");
        assertThat(ScopePreset.ALL.getLevel()).isEqualTo(100);
        assertThat(ScopePreset.SELF.getDisplayName()).isEqualTo("仅本人");
        assertThat(ScopePreset.SELF.getLevel()).isEqualTo(20);
        assertThat(ScopePreset.DEPARTMENT.getDisplayName()).isEqualTo("本组织");
        assertThat(ScopePreset.DEPARTMENT.getLevel()).isEqualTo(60);
        assertThat(ScopePreset.DEPARTMENT_AND_BELOW.getDisplayName()).isEqualTo("本组织及以下");
        assertThat(ScopePreset.DEPARTMENT_AND_BELOW.getLevel()).isEqualTo(80);
        assertThat(ScopePreset.MANAGED_ORGS.getDisplayName()).isEqualTo("我管理的组织");
        assertThat(ScopePreset.MANAGED_ORGS.getLevel()).isEqualTo(65);
        assertThat(ScopePreset.MANAGED_ORGS_AND_BELOW.getDisplayName()).isEqualTo("我管理的组织及以下");
        assertThat(ScopePreset.MANAGED_ORGS_AND_BELOW.getLevel()).isEqualTo(75);
        assertThat(ScopePreset.CUSTOM.getDisplayName()).isEqualTo("自定义");
        assertThat(ScopePreset.CUSTOM.getLevel()).isEqualTo(40);
    }

    /** Presets fill ONLY axis ① — applyTo and axes ②③ are layered by the caller, not the preset. */
    @Test
    void everyPreset_toSpecFillsOnlyAxisOne() {
        for (ScopePreset p : ScopePreset.values()) {
            ScopeSpec s = p.toSpec();
            assertThat(s).as("toSpec non-null for %s", p).isNotNull();
            assertThat(s.getOrgAnchor()).as("orgAnchor non-null for %s", p).isNotNull();
            // axis ① only: applyTo + axes ②③ left null for the caller to layer.
            assertThat(s.getApplyTo()).as("applyTo null for %s", p).isNull();
            assertThat(s.getCustomOrgIds()).as("customOrgIds null for %s", p).isNull();
            assertThat(s.getSubjectRelInclude()).as("subjectRelInclude null for %s", p).isNull();
            assertThat(s.getSubjectRelExclude()).as("subjectRelExclude null for %s", p).isNull();
            assertThat(s.getTypeFilter()).as("typeFilter null for %s", p).isNull();
        }
    }

    @Test
    void fromLegacy_knownAndUnknown() {
        assertThat(ScopePreset.fromLegacyScopeType("MANAGED_ORGS_AND_BELOW"))
                .isEqualTo(ScopePreset.MANAGED_ORGS_AND_BELOW);
        assertThat(ScopePreset.fromLegacyScopeType("BY_CLASS")).isNull();
        assertThat(ScopePreset.fromLegacyScopeType(null)).isNull();
    }

    @Test
    void fromLegacy_allSevenPresetNamesRoundTrip() {
        for (ScopePreset p : ScopePreset.values()) {
            assertThat(ScopePreset.fromLegacyScopeType(p.name())).isEqualTo(p);
        }
    }

    @Test
    void fromLegacy_pluginDimsAndUnknownReturnNull() {
        assertThat(ScopePreset.fromLegacyScopeType("BY_MAJOR")).isNull();
        assertThat(ScopePreset.fromLegacyScopeType("BY_GRADE")).isNull();
        assertThat(ScopePreset.fromLegacyScopeType("GIBBERISH")).isNull();
        assertThat(ScopePreset.fromLegacyScopeType("")).isNull();
    }
}
