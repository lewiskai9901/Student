package com.school.management.application.access;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FlowableRelationGrantBridgeTest {

    @Test
    void grant_callsForceGrantWithMappedFields() {
        AccessRelationService access = mock(AccessRelationService.class);
        when(access.forceGrant(any())).thenReturn(999L);

        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("subjectType")).thenReturn("user");
        when(execution.getVariable("subjectId")).thenReturn(1L);
        when(execution.getVariable("relation")).thenReturn("admin");
        when(execution.getVariable("resourceType")).thenReturn("org_unit");
        when(execution.getVariable("resourceId")).thenReturn(100L);
        when(execution.getVariable("requesterId")).thenReturn(7L);
        when(execution.getVariable("accessLevel")).thenReturn("FULL");

        FlowableRelationGrantBridge bridge = new FlowableRelationGrantBridge(access);
        bridge.grant(execution);

        verify(access).forceGrant(any());
        verify(execution).setVariable(eq("grantedRelationId"), eq(999L));
    }
}
