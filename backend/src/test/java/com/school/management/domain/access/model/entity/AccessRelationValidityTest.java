package com.school.management.domain.access.model.entity;

import com.school.management.domain.access.model.valueobject.AccessLevel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccessRelationValidityTest {

    @Test
    void noValidityWindow_alwaysActive() {
        AccessRelation r = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL)
            .build();
        assertThat(r.isCurrentlyActive()).isTrue();
        assertThat(r.isActiveAt(LocalDateTime.now().minusYears(10))).isTrue();
        assertThat(r.isActiveAt(LocalDateTime.now().plusYears(10))).isTrue();
    }

    @Test
    void validFromInPast_currentlyActive() {
        AccessRelation r = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .validFrom(LocalDateTime.now().minusDays(1))
            .build();
        assertThat(r.isCurrentlyActive()).isTrue();
    }

    @Test
    void validFromInFuture_notYetActive() {
        AccessRelation r = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .validFrom(LocalDateTime.now().plusDays(1))
            .build();
        assertThat(r.isCurrentlyActive()).isFalse();
    }

    @Test
    void validToInPast_expired() {
        AccessRelation r = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .validTo(LocalDateTime.now().minusDays(1))
            .build();
        assertThat(r.isCurrentlyActive()).isFalse();
    }

    @Test
    void validToInFuture_stillActive() {
        AccessRelation r = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .validTo(LocalDateTime.now().plusDays(1))
            .build();
        assertThat(r.isCurrentlyActive()).isTrue();
    }

    @Test
    void window_atSpecificTime() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        AccessRelation r = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .validFrom(base)
            .validTo(base.plusMonths(6))
            .build();
        assertThat(r.isActiveAt(base.minusDays(1))).isFalse();      // before window
        assertThat(r.isActiveAt(base.plusMonths(3))).isTrue();      // inside window
        assertThat(r.isActiveAt(base.plusMonths(6))).isFalse();     // exclusive on validTo
        assertThat(r.isActiveAt(base.plusMonths(7))).isFalse();     // after window
    }
}
