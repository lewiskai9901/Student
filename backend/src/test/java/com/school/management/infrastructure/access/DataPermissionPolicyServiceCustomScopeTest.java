package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CUSTOM 数据范围读路径回归 (T9 后).
 *
 * <p>T9 删除了旧 {@code scope_type}/{@code custom_org_unit_ids} 列及
 * {@code getMergedScope}→{@code loadCustomOrgUnitIds}→{@code MergedDataScope} 链。
 * 现 CUSTOM 范围的存储真相是轴① ({@code org_anchor='CUSTOM_ORG'} + {@code custom_org_ids} JSON);
 * 读路径走 {@code getRolePermissions}→{@code mapToPermission}, 由 {@code custom_org_ids}
 * 重建 {@link DataScopeItem} (统一大写 {@code ORG_UNIT}, 无旧大小写不一致 bug)。
 *
 * <p>本测试用 mock 的 {@link JdbcTemplate} 返回一行 CUSTOM 配置, 断言读回的
 * {@link RoleDataPermission} 的 scopeCode=CUSTOM 且 scopeItems 暴露配置的组织 id。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataPermissionPolicyService — CUSTOM scope 轴①读回")
class DataPermissionPolicyServiceCustomScopeTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ROLE_ID = 5L;
    private static final Long ORG_ID = 123L;
    private static final String MODULE_CODE = "student";

    private Map<String, Object> customRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1000L);
        row.put("role_id", ROLE_ID);
        row.put("resource_code", MODULE_CODE);
        row.put("apply_to", "BOTH");
        row.put("org_anchor", "CUSTOM_ORG");
        row.put("anchor_param", null);
        row.put("include_subtree", 0);
        row.put("custom_org_ids", "[123]");
        return row;
    }

    @Test
    @DisplayName("CUSTOM(custom_org_ids=[123]) 读回 scopeCode=CUSTOM 且 scopeItems 含 123")
    void customScopeRoundTripExposesOrgUnitId() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), anyLong(), anyLong()))
                .thenReturn(List.of(customRow()));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(customRow()));

        DataPermissionPolicyService service = new DataPermissionPolicyService(jdbcTemplate);

        List<RoleDataPermission> permissions = service.getRolePermissions(TENANT_ID, ROLE_ID);

        assertThat(permissions).hasSize(1);
        RoleDataPermission p = permissions.get(0);
        assertThat(p.getScopeCode())
                .as("CUSTOM_ORG 锚点应反推回命名码 CUSTOM")
                .isEqualTo("CUSTOM");
        assertThat(p.getScopeItems())
                .as("CUSTOM scope 必须由 custom_org_ids 暴露配置的组织 id")
                .extracting(DataScopeItem::getScopeId)
                .contains(ORG_ID);
        assertThat(p.getScopeItems())
                .extracting(DataScopeItem::getItemTypeCode)
                .containsOnly("ORG_UNIT");
    }
}
