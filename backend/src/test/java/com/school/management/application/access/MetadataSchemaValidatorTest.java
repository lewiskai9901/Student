package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataSchemaValidatorTest {

    private final MetadataSchemaValidator validator = new MetadataSchemaValidator(new ObjectMapper());

    @Test
    void unregisteredRelation_passesAnyMetadata() {
        // 未注册的关系 — 任何 metadata 都通过
        assertThatCode(() -> validator.validate("any_relation", null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("any_relation", Map.of("foo", "bar"))).doesNotThrowAnyException();
    }

    @Test
    void registeredRequired_rejectsMissing() {
        validator.registerRequiredKeys("occupies", "seat_no", "check_in_time");
        assertThatThrownBy(() -> validator.validate("occupies", Map.of("seat_no", "A101")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("check_in_time");
    }

    @Test
    void registeredRequired_acceptsAllPresent() {
        validator.registerRequiredKeys("occupies2", "seat_no", "check_in_time");
        assertThatCode(() -> validator.validate("occupies2",
            Map.of("seat_no", "A101", "check_in_time", "2026-01-01")))
            .doesNotThrowAnyException();
    }

    @Test
    void registeredRequired_rejectsNullMetadata() {
        validator.registerRequiredKeys("occupies3", "seat_no");
        assertThatThrownBy(() -> validator.validate("occupies3", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyRequired_isNoop() {
        // 空 required varargs 不注册, 视为未注册
        validator.registerRequiredKeys("watches");
        assertThatCode(() -> validator.validate("watches", null)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate("watches", Map.of())).doesNotThrowAnyException();
    }
}
