package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.repository.AccessRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * M1.2 — AccessRelationService.findSubjectsWithRelation() 直查 + 一层 implied 反展开.
 *
 * <p>W1.3 改名后, SQL 收回 AccessRelationRepository, 测试改用 mock repo.
 */
class AuthorizationServiceFindSubjectsTest {

    private AccessRelationRepository repo;
    private JdbcTemplate jdbc;
    private ApplicationEventPublisher events;
    private ObjectMapper om;
    private AccessRelationService svc;

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
        repo = mock(AccessRelationRepository.class);
        jdbc = mock(JdbcTemplate.class);
        events = mock(ApplicationEventPublisher.class);
        om = new ObjectMapper();
        svc = new AccessRelationService(repo, events, om,
            List.of(new FakeOccupantsDiscovery()), jdbc, noopCheckCache(), new MetadataSchemaValidator(om),
            new RelationApprovalService(jdbc, om, events));
    }

    private static AccessCheckCache noopCheckCache() {
        AccessCheckCache c = new AccessCheckCache(mock(com.school.management.infrastructure.cache.CacheService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(c, "enabled", false);
        org.springframework.test.util.ReflectionTestUtils.setField(c, "ttlSeconds", 60);
        return c;
    }

    /** relation_types 无 implied 声明 */
    private void stubNoImplied() {
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of());
    }

    @Test
    void findSubjectsWithRelation_directOnly_noImplied_whenExpandFalse() {
        stubNoImplied();

        // 直查: resource=user 777, viewer, subjectTypeFilter=null → [42, 99]
        when(repo.findActiveSubjectIds(eq("user"), eq(777L), eq("viewer"), isNull()))
            .thenReturn(List.of(42L, 99L));

        svc.bootImpliedCache();

        List<Long> ids = svc.findSubjectsWithRelation("user", 777L, "viewer", false);
        assertThat(ids).containsExactlyInAnyOrder(42L, 99L);
    }

    @Test
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

        // 1. 直查 viewer on user 777 with subject_type=user → [] (B 不是任何人直接的 viewer target)
        when(repo.findActiveSubjectIds(eq("user"), eq(777L), eq("viewer"), eq("user")))
            .thenReturn(List.of());

        // 2. 反查 source: place=123 上 manages 关系 subject_type=user → [42]
        when(repo.findActiveSubjectIds(eq("place"), eq(123L), eq("manages"), eq("user")))
            .thenReturn(List.of(42L));

        svc.bootImpliedCache();

        List<Long> ids = svc.findSubjectsWithRelation("user", 777L, "viewer", "user", true);
        assertThat(ids).containsExactly(42L);
    }

    @Test
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
        when(repo.findActiveSubjectIds(eq("user"), eq(999L), eq("viewer"), eq("user")))
            .thenReturn(List.of(55L));

        svc.bootImpliedCache();

        // user 999 没有 occupants 派生 (reverseDiscover 返 []), 只返直查
        List<Long> ids = svc.findSubjectsWithRelation("user", 999L, "viewer", "user", true);
        assertThat(ids).containsExactly(55L);
    }
}
