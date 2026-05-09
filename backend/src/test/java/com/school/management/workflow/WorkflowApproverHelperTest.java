package com.school.management.workflow;

import com.school.management.application.access.ApproverResolver;
import com.school.management.application.workflow.WorkflowApproverHelper;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkflowApproverHelperTest {

    @Test
    void findFirstApprover_returnsFirstId() {
        ApproverResolver resolver = mock(ApproverResolver.class);
        when(resolver.resolveApprovers(any())).thenReturn(List.of(100L, 200L));

        WorkflowApproverHelper h = new WorkflowApproverHelper(resolver);
        String result = h.findFirstApprover("admin", "org_unit", 50L, 7L);
        assertThat(result).isEqualTo("100");
    }

    @Test
    void findFirstApprover_emptyResult_returnsEmptyString() {
        ApproverResolver resolver = mock(ApproverResolver.class);
        when(resolver.resolveApprovers(any())).thenReturn(List.of());

        WorkflowApproverHelper h = new WorkflowApproverHelper(resolver);
        String result = h.findFirstApprover("admin", "org_unit", 50L, 7L);
        assertThat(result).isEmpty();
    }

    @Test
    void findApprovers_buildsCorrectPendingMock() {
        ApproverResolver resolver = mock(ApproverResolver.class);
        when(resolver.resolveApprovers(any())).thenReturn(List.of(123L));

        WorkflowApproverHelper h = new WorkflowApproverHelper(resolver);
        h.findApprovers("teaches", "org_unit", 50L, 7L);

        ArgumentCaptor<PendingRelationApproval> captor = ArgumentCaptor.forClass(PendingRelationApproval.class);
        verify(resolver).resolveApprovers(captor.capture());
        PendingRelationApproval p = captor.getValue();

        assertThat(p.getRelation()).isEqualTo("teaches");
        assertThat(p.getResourceType()).isEqualTo("org_unit");
        assertThat(p.getResourceId()).isEqualTo(50L);
        assertThat(p.getRequestedBy()).isEqualTo(7L);
        assertThat(p.getStatus()).isEqualTo(PendingRelationApproval.Status.PENDING);
        assertThat(p.getTenantId()).isEqualTo(1L);
    }
}
