package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 守护: InspProject 的 orgUnitId 字段语义 (横切关注点 + 显式 override).
 *
 * org_unit_id 是数据权限基础设施字段, 等同于 tenant_id / created_at / deleted —
 * 由 InspectionDataPermissionFiller (MetaObjectHandler) 在 INSERT 阶段自动填充
 * (默认 = SecurityContext.currentUser.orgUnitId). 业务可显式 override 用于
 * "集团 admin 审计某分公司项目" 这类跨组织场景.
 *
 * 因此聚合根层不强校验必填 — 校验在基础设施层 (handler 内 fail-fast / metric).
 */
@DisplayName("InspProject orgUnitId 字段语义守护")
class InspProjectOrgUnitTest {

    @Test
    @DisplayName("create 接受 null orgUnitId — 由基础设施层兜底填充")
    void createAcceptsNullOrgUnitId() {
        InspProject p = InspProject.create(
                "PRJ-1", "测试项目", 100L, LocalDate.of(2026, 3, 1),
                /* orgUnitId */ null,
                /* createdBy */ 999L);
        assertThat(p.getOrgUnitId()).isNull();
        assertThat(p.getStatus()).isEqualTo(ProjectStatus.DRAFT);
    }

    @Test
    @DisplayName("create 显式传入 orgUnitId 后聚合根可读回 (业务 override 通道)")
    void createKeepsExplicitOrgUnitIdInBuiltAggregate() {
        InspProject p = InspProject.create(
                "PRJ-1", "测试项目", 100L, LocalDate.of(2026, 3, 1),
                /* orgUnitId */ 50L,
                /* createdBy */ 999L);
        assertThat(p.getOrgUnitId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Builder 支持 orgUnitId 字段持久化往返")
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
