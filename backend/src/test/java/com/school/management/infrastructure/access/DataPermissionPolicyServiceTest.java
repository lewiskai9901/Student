package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DataPermissionPolicyService#getScopeSpec}.
 *
 * <p>Mocks {@link JdbcTemplate}; asserts the new composable {@link ScopeSpec} read path
 * builds the spec from the new axis columns (org_anchor / anchor_param / include_subtree /
 * type_filter / subject_rel_exclude / apply_to).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataPermissionPolicyServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DataPermissionPolicyService service;

    @BeforeEach
    void setUp() {
        service = new DataPermissionPolicyService(jdbcTemplate, new ChainValidator(new ResourceRelationRegistry(null), new com.school.management.application.access.RelationTypeRegistry(null)));
    }

    @Test
    @DisplayName("getScopeSpec: 轴① 从 relation_grants 反推 (R3a-2b: org_anchor 等列已废)")
    void getScopeSpec_buildsFromNewColumns() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("apply_to", "BOTH");
        // R3a-2b: 轴① 唯一来源是 relation_grants; getScopeSpec 不再读 org_anchor 等列
        row.put("relation_grants",
                "[{\"relation\":\"owner_org\",\"subject\":\"RELATION\",\"subjectParam\":\"admin\",\"subtree\":true}]");
        row.put("subject_rel_include", null);
        row.put("subject_rel_exclude", "[\"admin\"]");
        row.put("type_filter", "[\"STUDENT\"]");

        when(jdbcTemplate.queryForList(any(String.class), any(Object[].class)))
                .thenReturn(List.of(row));

        ScopeSpec spec = service.getScopeSpec(1L, 9L, "user", "READ");

        assertThat(spec).isNotNull();
        assertThat(spec.getOrgAnchor()).isEqualTo(OrgAnchor.RELATION);
        assertThat(spec.getAnchorParam()).isEqualTo("admin");
        assertThat(spec.isIncludeSubtree()).isTrue();
        assertThat(spec.getTypeFilter()).containsExactly("STUDENT");
        assertThat(spec.getSubjectRelExclude()).containsExactly("admin");
        assertThat(spec.getSubjectRelInclude()).isNull();
        assertThat(spec.getCustomOrgIds()).isNull();
        assertThat(spec.getApplyTo()).isEqualTo("BOTH");
    }

    @Test
    @DisplayName("getScopeSpec: no row → null")
    void getScopeSpec_noRow_returnsNull() {
        when(jdbcTemplate.queryForList(any(String.class), any(Object[].class)))
                .thenReturn(Collections.emptyList());

        ScopeSpec spec = service.getScopeSpec(1L, 9L, "user", "READ");

        assertThat(spec).isNull();
    }

    @Test
    @DisplayName("getScopeSpec: null org_anchor 收窄到 SELF (T9: scope_type 列已删, 不再翻译, 防御默认收窄)")
    void getScopeSpec_nullAnchor_defaultsToSelf() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("apply_to", "BOTH");
        row.put("org_anchor", null);
        row.put("anchor_param", null);
        row.put("include_subtree", 0);

        when(jdbcTemplate.queryForList(any(String.class), any(Object[].class)))
                .thenReturn(List.of(row));

        ScopeSpec spec = service.getScopeSpec(1L, 9L, "user", "READ");

        assertThat(spec).isNotNull();
        assertThat(spec.getOrgAnchor()).isEqualTo(OrgAnchor.SELF);
        assertThat(spec.isIncludeSubtree()).isFalse();
        assertThat(spec.getApplyTo()).isEqualTo("BOTH");
    }
}
