package com.school.management.application.inspection;

import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InspTaskQueryService 单元测试 (I7).
 *
 * <p>覆盖 governance KPI / inspection mode CRUD 路径:
 * <ul>
 *   <li>KPI 拼 SQL 时正确注入 scope clause</li>
 *   <li>inspection mode CRUD 校验非法值</li>
 *   <li>allow_ad_hoc 兜底返回 false</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspTaskQueryService — 运营 KPI / 项目模式")
class InspTaskQueryServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock InspectionScopeHelper scopeHelper;
    @InjectMocks InspTaskQueryService service;

    @Test
    @DisplayName("getTaskTypeKpi 5 维度 — 把 scope clause 拼进 WHERE")
    void getTaskTypeKpi_includesScopeClause() {
        when(scopeHelper.orgScopeClause("org_unit_id"))
                .thenReturn(" AND org_unit_id IN (5,6,7)");
        when(jdbcTemplate.queryForMap(any(String.class)))
                .thenReturn(emptyKpiRow());

        Map<String, Object> result = service.getTaskTypeKpi(100L);

        assertThat(result).containsKeys("scheduled", "adHoc", "triggered", "selfCheck", "crossAudit");
        // 验证 5 个 SQL 都拼了 scope 子句
        verify(jdbcTemplate, org.mockito.Mockito.times(5))
                .queryForMap(contains("org_unit_id IN (5,6,7)"));
    }

    @Test
    @DisplayName("getTaskTypeKpi projectId=null → 仍包含 scope, 不含 project_id 过滤")
    void getTaskTypeKpi_noProjectId_stillScoped() {
        when(scopeHelper.orgScopeClause("org_unit_id")).thenReturn(" AND 1=0");
        when(jdbcTemplate.queryForMap(any(String.class)))
                .thenReturn(emptyKpiRow());

        service.getTaskTypeKpi(null);

        verify(jdbcTemplate, org.mockito.Mockito.times(5))
                .queryForMap(contains("1=0"));
        verify(jdbcTemplate, org.mockito.Mockito.never())
                .queryForMap(contains("project_id ="));
    }

    @Test
    @DisplayName("listAdHocAllowedProjects — 拼 scope 子句")
    void listAdHocAllowedProjects_includesScope() {
        when(scopeHelper.orgScopeClause("org_unit_id"))
                .thenReturn(" AND org_unit_id IN (9)");
        when(jdbcTemplate.queryForList(any(String.class)))
                .thenReturn(List.of(Map.of("id", 1L)));

        service.listAdHocAllowedProjects();

        verify(jdbcTemplate).queryForList(contains("org_unit_id IN (9)"));
    }

    @Test
    @DisplayName("getInspectionMode 查不到 → 返回默认 PLANNED 结构")
    void getInspectionMode_notFound_returnsDefault() {
        when(jdbcTemplate.queryForMap(any(String.class), eq(99L)))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        Map<String, Object> mode = service.getInspectionMode(99L);

        assertThat(mode).containsEntry("inspection_mode", "PLANNED");
        assertThat(mode).containsEntry("allow_ad_hoc", 0);
        assertThat(mode).containsEntry("allow_self_check", 0);
    }

    @Test
    @DisplayName("updateInspectionMode — 非法 inspection_mode 拒绝")
    void updateInspectionMode_rejectsInvalidMode() {
        assertThatThrownBy(() -> service.updateInspectionMode(
                1L, "INVALID_MODE", true, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法的 inspection_mode");
    }

    @Test
    @DisplayName("updateInspectionMode — PLANNED 模式强制 adHoc=false")
    void updateInspectionMode_plannedForcesAdHocFalse() {
        Map<String, Object> stubRow = new HashMap<>();
        stubRow.put("inspection_mode", "PLANNED");
        stubRow.put("allow_ad_hoc", 0);
        stubRow.put("allow_self_check", 0);
        stubRow.put("ad_hoc_quota_per_inspector", null);
        lenient().when(jdbcTemplate.queryForMap(any(String.class), eq(1L)))
                .thenReturn(stubRow);

        service.updateInspectionMode(1L, "PLANNED", true, true, 5);

        // 验证 UPDATE 时 allow_ad_hoc 被强制为 0
        verify(jdbcTemplate).update(
                any(String.class),
                eq("PLANNED"), eq(0), eq(1), eq(5), eq(1L));
    }

    @Test
    @DisplayName("isProjectAllowAdHoc — DB 返回 1 → true")
    void isProjectAllowAdHoc_oneReturnsTrue() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), eq(10L)))
                .thenReturn(1);

        assertThat(service.isProjectAllowAdHoc(10L)).isTrue();
    }

    @Test
    @DisplayName("isProjectAllowAdHoc — DB 异常 → false (兜底, 不抛)")
    void isProjectAllowAdHoc_exceptionReturnsFalse() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), anyLong()))
                .thenThrow(new RuntimeException("DB down"));

        assertThat(service.isProjectAllowAdHoc(99L)).isFalse();
    }

    private Map<String, Object> emptyKpiRow() {
        Map<String, Object> m = new HashMap<>();
        m.put("total", 0);
        m.put("completed", 0);
        m.put("late", 0);
        m.put("uniqueInspectors", 0);
        m.put("last30d", 0);
        m.put("responded", 0);
        m.put("avgHours", null);
        m.put("submitted", 0);
        m.put("uniqueSubjects", 0);
        m.put("uniqueAuditors", 0);
        return m;
    }
}
