package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * W4.3 — AuthorizationService.check() 直接 + implied 推导 (BFS 多层递归)
 */
class AuthorizationServiceImpliedTest {

    private JdbcTemplate jdbc;
    private ApplicationEventPublisher events;
    private ObjectMapper om;
    private AuthorizationService svc;

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
        jdbc = mock(JdbcTemplate.class);
        events = mock(ApplicationEventPublisher.class);
        om = new ObjectMapper();
        svc = new AuthorizationService(jdbc, events, om,
            List.of(new FakeOccupantsDiscovery(),
                    new FakeMembersDiscovery(),
                    new FakeDescendantsDiscovery()));
    }

    /** 便捷: 构造"subject 直接关系"行, BFS 起点 */
    private static Map<String, Object> directRow(String resourceType, long resourceId, String relation) {
        return Map.of(
            "resource_type", resourceType,
            "resource_id", resourceId,
            "relation", relation
        );
    }

    /** stub: relation_types 无 implied 声明 */
    private void stubNoImplied() {
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of());
    }

    /** stub: subject 的直接关系 BFS 起点列表 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubDirectRelations(List<Map<String, Object>> rows) {
        when(jdbc.queryForList(
            ArgumentMatchers.contains("SELECT resource_type, resource_id, relation FROM access_relations"),
            any(Object[].class)))
            .thenReturn((List) rows);
    }

    @Test
    void check_directRelationHit_returnsTrue_withoutImpliedQuery() {
        // Direct checkDirect 命中
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(1);
        stubNoImplied();

        svc.bootImpliedCache();

        boolean ok = svc.check("user", 42L, "viewer", "user", 777L);
        assertThat(ok).isTrue();
    }

    @Test
    void check_noDirect_butImpliedViaDormHead_returnsTrue() {
        // checkDirect 不命中
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

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
        stubDirectRelations(List.of(directRow("place", 123L, "dorm_head")));

        svc.bootImpliedCache();

        // check: user 42 对 user 777 有 viewer? direct 不命中 → implied 命中 (777 在 place 123 occupants)
        boolean ok = svc.check("user", 42L, "viewer", "user", 777L);
        assertThat(ok).isTrue();

        // 反面: user 999 不在 occupants → 不命中
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    @Test
    void check_emptyImpliedIndex_fallsBackToFalse() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);
        stubNoImplied();

        svc.bootImpliedCache();

        assertThat(svc.check("user", 1L, "viewer", "user", 2L)).isFalse();
    }

    /**
     * W4.4 reference demo: CoreRelationsPlugin 的 manages(user→place) 声明了
     * implied viewer(user→user) via OCCUPANTS_OF_PLACE。
     */
    @Test
    void check_managesImpliesViewerOverOccupants_demoFromCorePlugin() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

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

        stubDirectRelations(List.of(directRow("place", 123L, "manages")));

        svc.bootImpliedCache();

        // userA(42) managed place 123, userB(777) occupies 123 → viewer 推导命中
        assertThat(svc.check("user", 42L, "viewer", "user", 777L)).isTrue();
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    @Test
    void checkDirect_bypassesImpliedExpansion() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);
        stubNoImplied();

        svc.bootImpliedCache();

        boolean ok = svc.checkDirect("user", 1L, "viewer", "user", 2L, LocalDateTime.now());
        assertThat(ok).isFalse();
        // 验证 checkDirect 不查 BFS 的 "SELECT resource_type, resource_id, relation"
        verify(jdbc, never()).queryForList(
            ArgumentMatchers.contains("SELECT resource_type, resource_id, relation"),
            any(Object[].class));
    }

    // ═══════════════════════════════════════════════════════════════════
    // 多层递归 BFS 测试
    // ═══════════════════════════════════════════════════════════════════

    /**
     * 两层链: admin(user→org) →[DESCENDANTS_OF_ORG] admin(user→org) →[MEMBERS_OF_ORG] viewer(user→user)
     * 用户对 org 10 有 admin, org 10 的子孙是 org 20, org 20 的 member 是 user 600
     * ⇒ check(user 42, viewer, user, 600) = true (via 两层 implied)
     */
    @Test
    void check_twoLayerImplied_hitsViaDescendantsThenMembers() {
        // checkDirect 不命中
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

        // implied 声明: 第一条 admin 派生 admin (子组织), 第二条 admin 派生 viewer (成员)
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

        // 用户 42 的直接关系: admin on org 10
        stubDirectRelations(List.of(directRow("org_unit", 10L, "admin")));

        svc.bootImpliedCache();

        // 两层: admin(org 10) → admin(org 20 via DESCENDANTS) → viewer(user 600 via MEMBERS)
        assertThat(svc.check("user", 42L, "viewer", "user", 600L)).isTrue();
        // 一层即命中: admin(org 10) → viewer(user 500 via MEMBERS)
        assertThat(svc.check("user", 42L, "viewer", "user", 500L)).isTrue();
        // 越界: user 999 既不是 org 10 也不是 org 20 的 member
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    /**
     * visited 去重: 构造自指派生 (admin → admin via DESCENDANTS),
     * 如果 discovery 返回自身或已访问节点, BFS 不应死循环.
     *
     * 这里 FakeDescendantsDiscovery.discover(10) = [20, 21],
     * 但我们虚构一条 admin 派生 admin 的规则 — 由于 discovery 不会 "回到" 已访问节点
     * (模拟 DAG), BFS 应在有限步终止.
     */
    @Test
    void check_selfReferentialImplied_terminatesWithoutInfiniteLoop() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(Map.of(
                "relation_code", "admin",
                "from_type", "user",
                "to_type", "org_unit",
                "implied_relations",
                "[{\"targetType\":\"org_unit\",\"relation\":\"admin\",\"discoveryRule\":\"DESCENDANTS_OF_ORG\"}]"
            )));

        stubDirectRelations(List.of(directRow("org_unit", 10L, "admin")));

        svc.bootImpliedCache();

        // org 99 不在任何 descendant 路径 → 不命中, 且 BFS 必须终止 (visited 防环)
        assertThat(svc.check("user", 42L, "admin", "org_unit", 99L)).isFalse();
        // org 20 是 org 10 的 descendant → 命中
        assertThat(svc.check("user", 42L, "admin", "org_unit", 20L)).isTrue();
    }

    /**
     * 深度限制: 构造会无限派生的 "恶意" discovery (每次返回一个新 id),
     * MAX_IMPLIED_DEPTH=5 应生效, 不死循环.
     */
    @Test
    void check_maxDepthLimit_preventsInfiniteExpansion() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

        // 换一个会"无限"产生新 id 的 discovery
        RelationDiscoveryRule infinite = new RelationDiscoveryRule() {
            public String code() { return "OCCUPANTS_OF_PLACE"; }
            public List<Long> discover(String type, Long id) {
                // 总是派生到下一个 place id (永远达不到 target 999)
                if (!"place".equals(type)) return List.of();
                return List.of(id + 1L);
            }
        };
        AuthorizationService svc2 = new AuthorizationService(jdbc, events, om, List.of(infinite));

        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(Map.of(
                "relation_code", "manages",
                "from_type", "user",
                "to_type", "place",
                "implied_relations",
                "[{\"targetType\":\"place\",\"relation\":\"manages\",\"discoveryRule\":\"OCCUPANTS_OF_PLACE\"}]"
            )));

        stubDirectRelations(List.of(directRow("place", 1L, "manages")));

        svc2.bootImpliedCache();

        // 目标 place 999 (超出 depth 能达到的范围) — BFS 在 MAX_DEPTH 处停止, 返 false
        long t0 = System.currentTimeMillis();
        boolean ok = svc2.check("user", 42L, "manages", "place", 999L);
        long elapsed = System.currentTimeMillis() - t0;
        assertThat(ok).isFalse();
        // 不应跑很久 — 5 层深度一次只派生一个 → 约 5 次 BFS 迭代
        assertThat(elapsed).as("BFS terminates fast due to depth cap").isLessThan(2000L);
    }
}
