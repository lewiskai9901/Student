package com.school.management.application.message;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.access.AccessRelationService;
import com.school.management.application.message.targetmode.ByRelationTargetMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M1.2 — BY_RELATION 走 AccessRelationService.findSubjectsWithRelation,
 * 不再裸 SQL — 确保 implied 派生关系的订阅者能收到.
 *
 * M2 后重构为直接测试 {@link ByRelationTargetMode} resolver.
 */
class MessageDispatcherImpliedTest {

    private AccessRelationService authService;
    private JdbcTemplate jdbcTemplate;
    private ByRelationTargetMode resolver;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        authService = mock(AccessRelationService.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        resolver = new ByRelationTargetMode(authService, jdbcTemplate);
        objectMapper = new ObjectMapper();
    }

    private Map<String, Object> parseConfig(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> eventMap(Long subjectId, String subjectType) {
        Map<String, Object> m = new HashMap<>();
        if (subjectId != null) m.put("subjectId", subjectId);
        if (subjectType != null) m.put("subjectType", subjectType);
        return m;
    }

    @Test
    void inward_callsAccessRelationServiceWithExpandImpliedTrue() throws Exception {
        when(authService.findSubjectsWithRelation(
            eq("place"), eq(101L), eq("viewer"), eq("user"), eq(true)))
            .thenReturn(List.of(42L, 77L));

        List<Long> ids = resolver.resolve(
            parseConfig("{\"relation\":\"viewer\",\"resource_type\":\"place\"}"),
            eventMap(101L, "PLACE"));

        assertThat(ids).containsExactlyInAnyOrder(42L, 77L);
        verify(authService).findSubjectsWithRelation("place", 101L, "viewer", "user", true);
    }

    @Test
    void inward_includeDeputies_callsAuthServiceTwice() throws Exception {
        when(authService.findSubjectsWithRelation(eq("org_unit"), eq(5L), eq("admin"), eq("user"), eq(true)))
            .thenReturn(List.of(1L, 2L));
        when(authService.findSubjectsWithRelation(eq("org_unit"), eq(5L), eq("deputy"), eq("user"), eq(true)))
            .thenReturn(List.of(3L));

        List<Long> ids = resolver.resolve(
            parseConfig("{\"relation\":\"admin\",\"resource_type\":\"org_unit\",\"include_deputies\":true}"),
            eventMap(5L, "ORG_UNIT"));

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void inward_returnsEmpty_whenSubjectIdNull() throws Exception {
        List<Long> ids = resolver.resolve(
            parseConfig("{\"relation\":\"viewer\"}"),
            eventMap(null, "USER"));
        assertThat(ids).isEmpty();
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), anyBoolean());
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void outward_doesNotCallAuthService_usesDirectJdbc() throws Exception {
        // outward 路径仍用裸 JDBC, 不会走 AccessRelationService
        resolver.resolve(
            parseConfig("{\"relation\":\"guardian_of\",\"resource_type\":\"user\",\"direction\":\"outward\"}"),
            eventMap(100L, "USER"));
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), anyBoolean());
        verify(authService, never()).findSubjectsWithRelation(
            any(), any(), any(), any(), anyBoolean());
    }
}
