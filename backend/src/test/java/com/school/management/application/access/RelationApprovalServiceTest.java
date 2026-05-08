package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import com.school.management.domain.access.model.entity.PendingRelationApproval.Status;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelationApprovalServiceTest {

    @Test
    void pendingApproval_isBuildable() {
        PendingRelationApproval p = PendingRelationApproval.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL)
            .status(Status.PENDING)
            .requestedBy(7L)
            .tenantId(1L)
            .build();
        assertThat(p.getStatus()).isEqualTo(Status.PENDING);
        assertThat(p.getAccessLevel()).isEqualTo(AccessLevel.FULL);
        assertThat(p.getRelation()).isEqualTo("admin");
    }

    @Test
    void status_lifecycle() {
        // PENDING → APPROVED 或 REJECTED 或 CANCELLED
        for (Status s : Status.values()) {
            assertThat(s).isNotNull();
        }
        assertThat(Status.values()).hasSize(4);
    }

    @Test
    void objectMapper_canSerializeMetadata() throws Exception {
        PendingRelationApproval p = PendingRelationApproval.builder()
            .relation("admin")
            .metadata(java.util.Map.of("key", "value"))
            .status(Status.PENDING)
            .build();
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(p.getMetadata());
        assertThat(json).contains("key").contains("value");
    }
}
