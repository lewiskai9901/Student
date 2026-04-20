package com.school.management.application.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * W4.3 — AuthorizationService.check() 直接 + implied 推导
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

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        events = mock(ApplicationEventPublisher.class);
        om = new ObjectMapper();
        svc = new AuthorizationService(jdbc, events, om,
            List.of(new FakeOccupantsDiscovery()));
    }

    @Test
    void check_directRelationHit_returnsTrue_withoutImpliedQuery() {
        // Direct checkDirect 命中
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(1);
        // refreshImpliedCache 加载时查不到 implied
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of());

        svc.bootImpliedCache();

        boolean ok = svc.check("user", 42L, "viewer", "user", 777L);
        assertThat(ok).isTrue();
    }

    @Test
    void check_noDirect_butImpliedViaDormHead_returnsTrue() {
        // checkDirect 不命中 → 返回 0
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

        // refreshImpliedCache: relation_types 里声明 dorm_head(user→place) 派生 viewer(user→user) via OCCUPANTS_OF_PLACE
        Map<String, Object> row = Map.of(
            "relation_code", "dorm_head",
            "from_type", "user",
            "to_type", "place",
            "implied_relations", "[{\"targetType\":\"user\",\"relation\":\"viewer\",\"discoveryRule\":\"OCCUPANTS_OF_PLACE\"}]"
        );
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of(row));

        // 用户 42 对 place 123 有 dorm_head 关系 → 上游 resource 列表返回 [123]
        when(jdbc.queryForList(
            ArgumentMatchers.contains("SELECT DISTINCT resource_id FROM access_relations"),
            eq(Long.class), any(Object[].class)))
            .thenReturn(List.of(123L));

        svc.bootImpliedCache();

        // check: user 42 对 user 777 有 viewer? direct 不命中 → implied 命中 (777 在 place 123 occupants)
        boolean ok = svc.check("user", 42L, "viewer", "user", 777L);
        assertThat(ok).isTrue();

        // 反面: user 42 对 user 999 (不在 occupants 里) → 不命中
        boolean notOk = svc.check("user", 42L, "viewer", "user", 999L);
        assertThat(notOk).isFalse();
    }

    @Test
    void check_emptyImpliedIndex_fallsBackToFalse() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);
        when(jdbc.queryForList(
            ArgumentMatchers.contains("WHERE is_enabled = 1 AND implied_relations IS NOT NULL")))
            .thenReturn(List.of());

        svc.bootImpliedCache();

        assertThat(svc.check("user", 1L, "viewer", "user", 2L)).isFalse();
    }

    /**
     * W4.4 reference demo: CoreRelationsPlugin 的 manages(user→place) 声明了
     * implied viewer(user→user) via OCCUPANTS_OF_PLACE。
     *
     * 场景: userA 是 place=101 的 manages, userB 是 place=101 的 occupies
     *  → check(userA viewer userB) 通过 implied 推导命中, 返回 true。
     *
     * 本测试 stub JdbcTemplate/discovery, 验证 CoreRelationsPlugin 声明
     * 能被 AuthorizationService 正确展开 (不依赖真实 DB)。
     */
    @Test
    void check_managesImpliesViewerOverOccupants_demoFromCorePlugin() {
        // 1. checkDirect 不命中 (userA 对 userB 没直接 viewer 关系)
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);

        // 2. impliedIndex 从 relation_types 加载到 manages(user→place) 的 implied
        //    (与 CoreRelationsPlugin.withImplied(...) 声明对齐)
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

        // 3. userA 对 place 123 有 manages 关系 → lookup 返回 [123]
        when(jdbc.queryForList(
            ArgumentMatchers.contains("SELECT DISTINCT resource_id FROM access_relations"),
            eq(Long.class), any(Object[].class)))
            .thenReturn(List.of(123L));

        // 4. FakeOccupantsDiscovery: place 123 → occupants [777, 888] (userB=777)
        svc.bootImpliedCache();

        // userA(42) managed place 123, userB(777) occupies 123 → viewer 推导命中
        assertThat(svc.check("user", 42L, "viewer", "user", 777L)).isTrue();

        // 反面: 随机 user 999 不在 occupants → 不命中
        assertThat(svc.check("user", 42L, "viewer", "user", 999L)).isFalse();
    }

    @Test
    void checkDirect_bypassesImpliedExpansion() {
        when(jdbc.queryForObject(
            ArgumentMatchers.contains("SELECT COUNT(1) FROM access_relations"),
            eq(Integer.class), any(Object[].class)))
            .thenReturn(0);
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        svc.bootImpliedCache();

        boolean ok = svc.checkDirect("user", 1L, "viewer", "user", 2L, LocalDateTime.now());
        assertThat(ok).isFalse();
        // 验证 checkDirect 不查 access_relations 的 DISTINCT resource_id (那是 implied 才用的)
        verify(jdbc, never()).queryForList(
            ArgumentMatchers.contains("SELECT DISTINCT resource_id"),
            eq(Long.class), any(Object[].class));
    }
}
