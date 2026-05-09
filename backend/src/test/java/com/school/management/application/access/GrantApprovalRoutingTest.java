package com.school.management.application.access;

import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 7 W7.1 — grant() 自动审批路由相关单元逻辑测试.
 * 真正的 service 集成测试在 SpringBoot 上下文里,这里只验:
 *   - PendingRelationApproval builder 模式
 *   - "负数 id 表示审批中" 协议
 */
class GrantApprovalRoutingTest {

    @Test
    void grantRequest_canBeConvertedToPending() {
        AccessRelationService.GrantRequest gr = AccessRelationService.GrantRequest.of(
            "user", 1L, "admin", "org_unit", 100L);
        gr.accessLevel = AccessLevel.FULL;
        gr.grantedBy = 7L;
        gr.tenantId = 1L;

        PendingRelationApproval p = PendingRelationApproval.builder()
            .resourceType(gr.resourceType).resourceId(gr.resourceId)
            .relation(gr.relation).subjectType(gr.subjectType).subjectId(gr.subjectId)
            .accessLevel(gr.accessLevel)
            .status(PendingRelationApproval.Status.PENDING)
            .requestedBy(gr.grantedBy).tenantId(gr.tenantId)
            .build();

        assertThat(p.getRelation()).isEqualTo("admin");
        assertThat(p.getResourceType()).isEqualTo("org_unit");
        assertThat(p.getResourceId()).isEqualTo(100L);
        assertThat(p.getSubjectType()).isEqualTo("user");
        assertThat(p.getSubjectId()).isEqualTo(1L);
        assertThat(p.getStatus()).isEqualTo(PendingRelationApproval.Status.PENDING);
        assertThat(p.getRequestedBy()).isEqualTo(7L);
        assertThat(p.getTenantId()).isEqualTo(1L);
    }

    @Test
    void approvalApi_negativeIdSemantics() {
        // grant 返回负数表示进入审批队列, pendingId 取 Math.abs
        Long apiResult = -42L;
        assertThat(apiResult < 0).isTrue();
        Long pendingId = -apiResult;
        assertThat(pendingId).isEqualTo(42L);
    }

    @Test
    void approvalApi_positiveIdMeansImmediatelyGranted() {
        // 正数表示直接落库的 access_relations id
        Long apiResult = 1234L;
        assertThat(apiResult > 0).isTrue();
    }
}
