package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterization test for the CUSTOM data-scope case-mismatch bug in
 * {@link DataPermissionPolicyService}.
 *
 * <p>Background: {@code loadCustomOrgUnitIds(...)} rebuilds {@link com.school.management.domain.access.model.entity.DataScopeItem}
 * with {@code itemTypeCode = "org_unit"} (lowercase), but
 * {@link MergedDataScope#getOrgUnitIds()} reads {@code getScopeIds("ORG_UNIT")} (uppercase),
 * and {@code MergedDataScope.merge} buckets items by {@code item.getItemTypeCode()}.
 * The items therefore land under map key {@code "org_unit"} while the reader looks up
 * {@code "ORG_UNIT"} → empty → the interceptor emits {@code 1 = 0} → CUSTOM denies everything.
 *
 * <p>This test exercises the real save→load mapping chain
 * ({@code getMergedScope} → {@code loadCustomOrgUnitIds} → {@code MergedDataScope.merge}
 * → {@code getOrgUnitIds}) with a mocked {@link JdbcTemplate} returning a CUSTOM row,
 * matching the Mockito-on-JdbcTemplate pattern established by
 * {@code DataPermissionApplicationServiceTest} and {@code DataPermissionInterceptorTest}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DataPermissionPolicyService — CUSTOM scope org_unit 类型大小写")
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
        row.put("scope_type", "CUSTOM");
        row.put("custom_org_unit_ids", "[123]");
        return row;
    }

    @Test
    @DisplayName("保存 CUSTOM(ORG_UNIT=123) 后 getMergedScope().getOrgUnitIds() 必须包含 123")
    void customScopeRoundTripExposesOrgUnitId() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(customRow()));

        DataPermissionPolicyService service = new DataPermissionPolicyService(jdbcTemplate);

        MergedDataScope merged = service.getMergedScope(TENANT_ID, List.of(ROLE_ID), MODULE_CODE);

        assertThat(merged.getEffectiveScope()).isEqualTo(DataScope.CUSTOM);
        // The bug: items bucketed under "org_unit" (lowercase) so getOrgUnitIds() (reads "ORG_UNIT") is empty.
        assertThat(merged.getOrgUnitIds())
                .as("CUSTOM scope must expose its configured org unit ids via getOrgUnitIds()")
                .contains(ORG_ID);
    }
}
