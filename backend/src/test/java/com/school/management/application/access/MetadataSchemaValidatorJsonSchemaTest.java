package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phase 7 W7.2 — 验证 networknt JSON Schema 完整库已接入并工作.
 */
class MetadataSchemaValidatorJsonSchemaTest {

    private final MetadataSchemaValidator v = new MetadataSchemaValidator(new ObjectMapper());

    @Test
    void fullSchema_typeValidation() {
        v.registerSchema("occupies", """
            {
              "type": "object",
              "required": ["seat_no", "check_in_time"],
              "properties": {
                "seat_no": {"type": "string"},
                "check_in_time": {"type": "string"}
              }
            }""");

        // OK
        v.validate("occupies", Map.of("seat_no", "A101", "check_in_time", "2026-01-01"));

        // 类型错(seat_no 应是 string,塞 number)
        assertThatThrownBy(() -> v.validate("occupies",
                Map.of("seat_no", 123, "check_in_time", "2026-01-01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("seat_no");
    }

    @Test
    void schema_enumValidation() {
        v.registerSchema("teaches", """
            {
              "type": "object",
              "properties": {
                "role": {"type": "string", "enum": ["lecturer", "ta"]}
              }
            }""");

        v.validate("teaches", Map.of("role", "lecturer"));

        assertThatThrownBy(() -> v.validate("teaches", Map.of("role", "professor")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void schema_unregisteredRelation_passes() {
        assertThatCode(() -> v.validate("any", Map.of("foo", 1)))
            .doesNotThrowAnyException();
    }
}
