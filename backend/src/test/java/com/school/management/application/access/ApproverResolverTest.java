package com.school.management.application.access;

import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.infrastructure.extension.ApproverFinderPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 7 W7.4 — ApproverResolver 路由测试.
 */
class ApproverResolverTest {

    @Test
    void specificFinder_winsOverFallback() {
        ApproverFinderPlugin admin = new ApproverFinderPlugin() {
            @Override public List<String> applicableRelations() { return List.of("admin"); }
            @Override public List<Long> findApprovers(PendingRelationApproval p) { return List.of(100L); }
            @Override public int order() { return 10; }
        };
        ApproverFinderPlugin fallback = new ApproverFinderPlugin() {
            @Override public List<String> applicableRelations() { return null; }
            @Override public List<Long> findApprovers(PendingRelationApproval p) { return List.of(999L); }
            @Override public int order() { return Integer.MAX_VALUE; }
        };
        ApproverResolver resolver = new ApproverResolver(List.of(fallback, admin));
        PendingRelationApproval p = PendingRelationApproval.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .requestedBy(7L).build();
        assertThat(resolver.resolveApprovers(p)).containsExactly(100L);
    }

    @Test
    void noMatch_fallsBackToGlobal() {
        ApproverFinderPlugin fallback = new ApproverFinderPlugin() {
            @Override public List<String> applicableRelations() { return null; }
            @Override public List<Long> findApprovers(PendingRelationApproval p) { return List.of(999L); }
        };
        ApproverResolver resolver = new ApproverResolver(List.of(fallback));
        PendingRelationApproval p = PendingRelationApproval.builder()
            .relation("anything").requestedBy(7L).build();
        assertThat(resolver.resolveApprovers(p)).containsExactly(999L);
    }

    @Test
    void noFinders_returnsEmpty() {
        ApproverResolver resolver = new ApproverResolver(List.of());
        PendingRelationApproval p = PendingRelationApproval.builder()
            .relation("x").requestedBy(7L).build();
        assertThat(resolver.resolveApprovers(p)).isEmpty();
    }
}
