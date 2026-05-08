package com.school.management.application.access.masking;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultMaskingPolicyTest {
    private final DefaultMaskingPolicy p = new DefaultMaskingPolicy();

    @Test
    void selfView_returnsEmpty() {
        assertThat(p.fieldsToMask(7L, 7L, Set.of())).isEmpty();
    }

    @Test
    void privilegedRelation_returnsEmpty() {
        assertThat(p.fieldsToMask(1L, 2L, Set.of("admin"))).isEmpty();
        assertThat(p.fieldsToMask(1L, 2L, Set.of("responsible_for"))).isEmpty();
        assertThat(p.fieldsToMask(1L, 2L, Set.of("family_of", "member"))).isEmpty();
    }

    @Test
    void peerRelation_masksPhoneEmail() {
        assertThat(p.fieldsToMask(1L, 2L, Set.of("member"))).containsExactlyInAnyOrder("phone", "email");
        assertThat(p.fieldsToMask(1L, 2L, Set.of("viewer"))).containsExactlyInAnyOrder("phone", "email");
    }

    @Test
    void noRelation_masksAll() {
        assertThat(p.fieldsToMask(1L, 2L, Set.of()))
            .containsExactlyInAnyOrder("phone", "email", "idCard", "realName");
        assertThat(p.fieldsToMask(1L, 2L, null))
            .containsExactlyInAnyOrder("phone", "email", "idCard", "realName");
    }
}
