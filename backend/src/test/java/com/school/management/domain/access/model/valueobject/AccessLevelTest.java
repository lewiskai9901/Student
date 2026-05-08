package com.school.management.domain.access.model.valueobject;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AccessLevelTest {

    @Test
    void parse_handlesNull() {
        assertThat(AccessLevel.parse(null)).isEqualTo(AccessLevel.FULL);
    }

    @Test
    void parse_handlesBlank() {
        assertThat(AccessLevel.parse("  ")).isEqualTo(AccessLevel.FULL);
        assertThat(AccessLevel.parse("")).isEqualTo(AccessLevel.FULL);
    }

    @Test
    void parse_caseInsensitive() {
        assertThat(AccessLevel.parse("read_only")).isEqualTo(AccessLevel.READ_ONLY);
        assertThat(AccessLevel.parse("READ_ONLY")).isEqualTo(AccessLevel.READ_ONLY);
        assertThat(AccessLevel.parse("Owner")).isEqualTo(AccessLevel.OWNER);
        assertThat(AccessLevel.parse("FULL")).isEqualTo(AccessLevel.FULL);
    }

    @Test
    void parse_invalidFallsBackToFull() {
        assertThat(AccessLevel.parse("WHATEVER")).isEqualTo(AccessLevel.FULL);
        assertThat(AccessLevel.parse("123")).isEqualTo(AccessLevel.FULL);
    }

    @Test
    void isReadWrite_correctness() {
        assertThat(AccessLevel.READ_ONLY.isReadWrite()).isFalse();
        assertThat(AccessLevel.FULL.isReadWrite()).isTrue();
        assertThat(AccessLevel.OWNER.isReadWrite()).isTrue();
    }

    @Test
    void canDelegate_onlyOwner() {
        assertThat(AccessLevel.OWNER.canDelegate()).isTrue();
        assertThat(AccessLevel.FULL.canDelegate()).isFalse();
        assertThat(AccessLevel.READ_ONLY.canDelegate()).isFalse();
    }
}
