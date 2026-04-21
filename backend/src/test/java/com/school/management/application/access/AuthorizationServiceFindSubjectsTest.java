package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M1.2 — AuthorizationService.findSubjectsWithRelation() 直查 + 一层 implied 反展开.
 */
class AuthorizationServiceFindSubjectsTest {

    private JdbcTemplate jdbc;
    private ApplicationEventPublisher events;
    private ObjectMapper om;
    private AuthorizationService svc;

    /** reverseDiscover: target=user 777 的 place 反查 → [123] (user 777 occupies place 123) */
    static class FakeOccupantsDiscovery implements RelationDiscoveryRule {
        public String code() { return "OCCUPANTS_OF_PLACE"; }
        public List<Long> discover(String type, Long id) { return List.of(); }
        @Override
        public List<Long> reverseDiscover(String type, Long id) {
            if ("user".equals(type) && id == 777L) return List.of(123L);
            return List.of();
        }
    }

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        events = mock(ApplicationEventPublisher.class);
        om = new ObjectMapper();
        svc = new AuthorizationService(jdbc, events, om,
            List.of(new FakeOccupantsDiscovery()));
    }

    /** relation_types 无 implied 声明 */
    private void stubNoImplied() {
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void findSubjectsWithRelation_directOnly_noImplied_whenExpandFalse() {
        stubNoImplied();

        // 直查: user=user 777, viewer → [42, 99]
        when(jdbc.queryForList(
            ArgumentMatchers.contains("SELECT DISTINCT subject_id FROM access_relations"),
            eq(Long.class),
            eq("user"), eq(777L), eq("viewer")))
            .thenReturn((List) List.of(42L, 99L));

        svc.bootImpliedCache();

        List<Long> ids = svc.findSubjectsWithRelation("user", 777L, "viewer", false);
        assertThat(ids).containsExactlyInAnyOrder(42L, 99L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void findSubjectsWithRelation_expandImplied_mergesDirectAndDerived() {
        // implied 声明: manages(user→place) 派生 viewer(user→user) via OCCUPANTS_OF_PLACE
        Map<String, Object> row = Map.of(
            "relation_code", "manages",
            "from_type", "user",
            "to_type", "place",
            "implied_relations",
            "[{\"targetType\":\"user\",\"relation\":\"viewer\",\"discoveryRule\":\"OCCUPANTS_OF_PLACE\"}]"
        );
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(row));

        // 1. 直查 viewer on user 777 with subject_type=user → []  (B 不是任何人直接的 viewer target)
        when(jdbc.queryForList(
            ArgumentMatchers.contains("SELECT DISTINCT subject_id FROM access_relations"),
            eq(Long.class),
            eq("user"), eq(777L), eq("viewer"), eq("user")))
            .thenReturn((List) List.of());

        // 2. 反查 source: place=123 上 manages 关系 subject_type=user → [42] (user A)
        when(jdbc.queryForList(
            ArgumentMatchers.contains("relation = ? AND subject_type = ?"),
            eq(Long.class),
            eq("place"), eq(123L), eq("manages"), eq("user")))
            .thenReturn((List) List.of(42L));

        svc.bootImpliedCache();

        // 期望: 通过 manages → viewer via OCCUPANTS_OF_PLACE (reverse: user 777 的 place = 123), user 42 被加入
        List<Long> ids = svc.findSubjectsWithRelation("user", 777L, "viewer", "user", true);
        assertThat(ids).containsExactly(42L);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void findSubjectsWithRelation_expandImplied_noReverseMatch_returnsDirectOnly() {
        Map<String, Object> row = Map.of(
            "relation_code", "manages",
            "from_type", "user",
            "to_type", "place",
            "implied_relations",
            "[{\"targetType\":\"user\",\"relation\":\"viewer\",\"discoveryRule\":\"OCCUPANTS_OF_PLACE\"}]"
        );
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(row));

        // 直查: 直接有 viewer [55]
        when(jdbc.queryForList(
            ArgumentMatchers.contains("SELECT DISTINCT subject_id FROM access_relations"),
            eq(Long.class),
            eq("user"), eq(999L), eq("viewer"), eq("user")))
            .thenReturn((List) List.of(55L));

        svc.bootImpliedCache();

        // user 999 没有 occupants 派生 (reverseDiscover 返 []), 只返直查
        List<Long> ids = svc.findSubjectsWithRelation("user", 999L, "viewer", "user", true);
        assertThat(ids).containsExactly(55L);
    }
}
