package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 守护: InspProject 创建必须显式提供 orgUnitId (覆盖范围的 org root).
 *
 * 语义: org_unit_id 是数据权限过滤列, 标记"该项目覆盖到哪个组织单元".
 * 不是创建者归属, 而是被检对象边界. Migration V20260501_1 已说明:
 * 项目级别的 org_unit_id 用于过滤 — 集团审计某分公司项目, 这里就传分公司 ID.
 *
 * Null = 数据权限边界丢失 → 整个 inspection 链路下游 (task/submission/evidence)
 * 都无法继承到准确的 org_unit, DataPermissionInterceptor 漏过滤 = 越权读.
 */
@DisplayName("InspProject orgUnitId 必填守护")
class InspProjectOrgUnitTest {

    @Test
    @DisplayName("create 不传 orgUnitId 抛 IllegalArgumentException")
    void createRejectsNullOrgUnitId() {
        assertThatThrownBy(() -> InspProject.create(
                "PRJ-1", "测试项目", 100L, LocalDate.of(2026, 3, 1),
                /* orgUnitId */ null,
                /* createdBy */ 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orgUnitId");
    }

    @Test
    @DisplayName("create 传入合法 orgUnitId 后聚合根可读回")
    void createKeepsOrgUnitIdInBuiltAggregate() {
        InspProject p = InspProject.create(
                "PRJ-1", "测试项目", 100L, LocalDate.of(2026, 3, 1),
                /* orgUnitId */ 50L,
                /* createdBy */ 999L);
        assertThat(p.getOrgUnitId()).isEqualTo(50L);
        assertThat(p.getStatus()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Test
    @DisplayName("Builder 也必须支持 orgUnitId 字段持久化")
    void builderRoundTripsOrgUnitId() {
        InspProject p = InspProject.reconstruct(InspProject.builder()
                .id(1L)
                .projectCode("PRJ-1")
                .projectName("X")
                .rootSectionId(100L)
                .orgUnitId(77L)
                .startDate(LocalDate.of(2026, 3, 1))
                .status(ProjectStatus.DRAFT)
                .createdBy(999L));
        assertThat(p.getOrgUnitId()).isEqualTo(77L);
    }
}
