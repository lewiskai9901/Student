package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.access.repository.AccessRelationRepository.DirectRelationRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * W4.3 — AccessRelationService.check() 直接 + implied 推导 (BFS 多层递归).
 *
 * <p>W1.3 改名后, SQL 收回到 AccessRelationRepository, 测试改用 mock repo.
 * relation_types 配置查询仍走 JdbcTemplate (refresh 逻辑保留).
 */
class AuthorizationServiceImpliedTest {

    private AccessRelationRepository repo;
    private JdbcTemplate jdbc;
    private ApplicationEventPublisher events;
    private ObjectMapper om;
    private AccessRelationService svc;

    /** 假 discovery: place 123 包含 occupants [777, 888] */
    static class FakeOccupantsDiscovery implements RelationDiscoveryRule {
        public String code() { return "OCCUPANTS_OF_PLACE"; }
        public List<Long> discover(String type, Long id) {
            if ("place".equals(type) && id == 123L) return List.of(777L, 888L);
            return List.of();
        }
    }

    /** 假 discovery: org 10 下 member [500], org 20 下 member [600] */
    static class FakeMembersDiscovery implements RelationDiscoveryRule {
        public String code() { return "MEMBERS_OF_ORG"; }
        public List<Long> discover(String type, Long id) {
            if (!"org_unit".equals(type)) return List.of();
            if (id == 10L) return List.of(500L);
            if (id == 20L) return List.of(600L);
            return List.of();
        }
    }

    /** 假 discovery: org 10 的子孙 [20, 21] */
    static class FakeDescendantsDiscovery implements RelationDiscoveryRule {
        public String code() { return "DESCENDANTS_OF_ORG"; }
        public List<Long> discover(String type, Long id) {
            if (!"org_unit".equals(type)) return List.of();
            if (id == 10L) return List.of(20L, 21L);
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
            List.of(new FakeOccupantsDiscovery(),
                    new FakeMembersDiscovery(),
                    new FakeDescendantsDiscovery()),
            jdbc, noopCheckCache(), new MetadataSchemaValidator(om),
            new RelationApprovalService(jdbc, om, events));
    }

    /** Test helper: AccessCheckCache with caching disabled — pure pass-through to loader. */
    private static AccessCheckCache noopCheckCache() {
        AccessCheckCache c = new AccessCheckCache(mock(com.school.management.infrastructure.cache.CacheService.class));
        org.springframework.test.util.ReflectionTestUtils.setField(c, "enabled", false);
        org.springframework.test.util.ReflectionTestUtils.setField(c, "ttlSeconds", 60);
        return c;
    }

    /** stub: relation_types 无 implied 声明 */
    private void stubNoImplied() {
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of());
    }

    /** stub: subject 的直接关系 BFS 起点列表 */
    private void stubDirectRelations(List<DirectRelationRef> rows) {
        when(repo.findActiveDirectRelationsBySubject(anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(rows);
    }

    @Test
    void check_directRelationHit_returnsTrue_withoutImpliedQuery() {
        // Direct checkDirect 命中
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(true);
        stubNoImplied();

        svc.bootImpliedCache();

        boolean ok = svc.check("user", 42L, "viewer", "user", 777L);
        assertThat(ok).isTrue();
    }

    @Test
    void check_noDirect_butImpliedViaDormHead_returnsTrue() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);

        // relation_types 里声明 dorm_head(user→place) 派生 viewer(user→user) via OCCUPANTS_OF_PLACE
        Map<String, Object> row = Map.of(
            "relation_code", "dorm_head",
            "from_type", "user",
            "to_type", "place",
            "implied_relations", "[{\"targetType\":\"user\",\"relation\":\"viewer\",\"discoveryRule\":\"OCCUPANTS_OF_PLACE\"}]"
        );
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(row));

        // 用户 42 的直接关系: dorm_head → place 123
        stubDirectRelations(List.of(new DirectRelationRef("place", 123L, "dorm_head")));

        svc.bootImpliedCache();

        // check: user 42 对 user 777 有 viewer? direct 不命中 → implied 命中 (777 在 place 123 occupants)
        boolean ok = svc.check("user", 42L, "viewer", "user", 777L);
        assertThat(ok).isTrue();

        // 反面: user 999 不在 occupants → 不命中
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    @Test
    void check_emptyImpliedIndex_fallsBackToFalse() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);
        stubNoImplied();

        svc.bootImpliedCache();

        assertThat(svc.check("user", 1L, "viewer", "user", 2L)).isFalse();
    }

    /**
     * W4.4 reference demo: CoreManifest.contribute() 的 manages(user→place) 声明了
     * implied viewer(user→user) via OCCUPANTS_OF_PLACE。
     */
    @Test
    void check_managesImpliesViewerOverOccupants_demoFromCorePlugin() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);

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

        stubDirectRelations(List.of(new DirectRelationRef("place", 123L, "manages")));

        svc.bootImpliedCache();

        assertThat(svc.check("user", 42L, "viewer", "user", 777L)).isTrue();
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    @Test
    void checkDirect_bypassesImpliedExpansion() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);
        stubNoImplied();

        svc.bootImpliedCache();

        boolean ok = svc.checkDirect("user", 1L, "viewer", "user", 2L, LocalDateTime.now());
        assertThat(ok).isFalse();
        // checkDirect 不应触发 BFS 起点查询
        verify(repo, never()).findActiveDirectRelationsBySubject(anyString(), any(), any(LocalDateTime.class));
    }

    /**
     * 两层链: admin(user→org) →[DESCENDANTS_OF_ORG] admin(user→org) →[MEMBERS_OF_ORG] viewer(user→user)
     */
    @Test
    void check_twoLayerImplied_hitsViaDescendantsThenMembers() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);

        List<Map<String, Object>> relTypeRows = new ArrayList<>();
        relTypeRows.add(Map.of(
            "relation_code", "admin",
            "from_type", "user",
            "to_type", "org_unit",
            "implied_relations",
            "[{\"targetType\":\"org_unit\",\"relation\":\"admin\",\"discoveryRule\":\"DESCENDANTS_OF_ORG\"}," +
            " {\"targetType\":\"user\",\"relation\":\"viewer\",\"discoveryRule\":\"MEMBERS_OF_ORG\"}]"
        ));
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(relTypeRows);

        stubDirectRelations(List.of(new DirectRelationRef("org_unit", 10L, "admin")));

        svc.bootImpliedCache();

        assertThat(svc.check("user", 42L, "viewer", "user", 600L)).isTrue();
        assertThat(svc.check("user", 42L, "viewer", "user", 500L)).isTrue();
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    /**
     * visited 去重: 自指派生 (admin → admin via DESCENDANTS),
     * BFS 在有限步终止 (DAG 模拟).
     */
    @Test
    void check_selfReferentialImplied_terminatesWithoutInfiniteLoop() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);

        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(Map.of(
                "relation_code", "admin",
                "from_type", "user",
                "to_type", "org_unit",
                "implied_relations",
                "[{\"targetType\":\"org_unit\",\"relation\":\"admin\",\"discoveryRule\":\"DESCENDANTS_OF_ORG\"}]"
            )));

        stubDirectRelations(List.of(new DirectRelationRef("org_unit", 10L, "admin")));

        svc.bootImpliedCache();

        assertThat(svc.check("user", 42L, "admin", "org_unit", 99L)).isFalse();
        assertThat(svc.check("user", 42L, "admin", "org_unit", 20L)).isTrue();
    }

    /**
     * 深度限制: 构造会无限派生的 "恶意" discovery (每次返回一个新 id),
     * MAX_IMPLIED_DEPTH=5 应生效, 不死循环.
     */
    @Test
    void check_maxDepthLimit_preventsInfiniteExpansion() {
        when(repo.checkDirectActive(anyString(), any(), anyString(), anyString(), any(), any(LocalDateTime.class)))
            .thenReturn(false);

        RelationDiscoveryRule infinite = new RelationDiscoveryRule() {
            public String code() { return "OCCUPANTS_OF_PLACE"; }
            public List<Long> discover(String type, Long id) {
                if (!"place".equals(type)) return List.of();
                return List.of(id + 1L);
            }
        };
        AccessRelationService svc2 = new AccessRelationService(repo, events, om, List.of(infinite), jdbc, noopCheckCache(), new MetadataSchemaValidator(om), new RelationApprovalService(jdbc, om, events));

        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(Map.of(
                "relation_code", "manages",
                "from_type", "user",
                "to_type", "place",
                "implied_relations",
                "[{\"targetType\":\"place\",\"relation\":\"manages\",\"discoveryRule\":\"OCCUPANTS_OF_PLACE\"}]"
            )));

        when(repo.findActiveDirectRelationsBySubject(eq("user"), eq(42L), any(LocalDateTime.class)))
            .thenReturn(List.of(new DirectRelationRef("place", 1L, "manages")));

        svc2.bootImpliedCache();

        long t0 = System.currentTimeMillis();
        boolean ok = svc2.check("user", 42L, "manages", "place", 999L);
        long elapsed = System.currentTimeMillis() - t0;
        assertThat(ok).isFalse();
        assertThat(elapsed).as("BFS terminates fast due to depth cap").isLessThan(2000L);
    }
}
