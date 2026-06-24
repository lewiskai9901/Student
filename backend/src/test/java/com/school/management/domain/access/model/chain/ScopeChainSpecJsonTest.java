package com.school.management.domain.access.model.chain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScopeChainSpec JSON 往返单测 (统一锚定 P0) —— 链要存进 {@code role_data_scopes.relation_grants} JSON,
 * 必须能无损序列化/反序列化。records + 参数名 (Spring Boot -parameters) → Jackson 直接编解码。
 */
class ScopeChainSpecJsonTest {

    private final ObjectMapper om = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("用户原例链 JSON 往返无损 (hops AND + 多终端 + 类型)")
    void roundTripUserExample() throws Exception {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(new Hop(List.of("admin", "responsible_for"), Combine.AND, "org_unit", true)),
                        new Terminal(List.of("owner_org", "taught_by"), Combine.AND),
                        List.of("STUDENT"))));

        String json = om.writeValueAsString(spec);
        ScopeChainSpec back = om.readValue(json, ScopeChainSpec.class);

        assertEquals(spec, back, "记录值相等 = 往返无损");
        // 关键字段抽查
        Chain c = back.chains().get(0);
        assertEquals(List.of("admin", "responsible_for"), c.hops().get(0).relations());
        assertEquals(Combine.AND, c.hops().get(0).combine());
        assertTrue(c.hops().get(0).subtree());
        assertEquals("org_unit", c.hops().get(0).toType());
        assertEquals(List.of("owner_org", "taught_by"), c.terminal().anchorRelations());
        assertEquals(Combine.AND, c.terminal().combine());
        assertEquals(List.of("STUDENT"), c.typeFilter());
    }

    @Test
    @DisplayName("1 跳退化链 (空 hops) JSON 往返无损")
    void roundTripOneHop() throws Exception {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ScopeChainSpec back = om.readValue(om.writeValueAsString(spec), ScopeChainSpec.class);
        assertEquals(spec, back);
        assertTrue(back.chains().get(0).hops().isEmpty());
    }

    @Test
    @DisplayName("多链 OR JSON 往返无损")
    void roundTripMultiChain() throws Exception {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("creator"), Combine.OR), List.of()),
                new Chain(
                        List.of(new Hop(List.of("manages"), Combine.OR, "place", false)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ScopeChainSpec back = om.readValue(om.writeValueAsString(spec), ScopeChainSpec.class);
        assertEquals(spec, back);
        assertEquals(2, back.chains().size());
    }

    @Test
    @DisplayName("[P-M1] 旧 grant JSON 无 direction 字段 → 反序列化默认 FORWARD (回兼, 金标准安全)")
    void legacyJsonWithoutDirectionDefaultsForward() throws Exception {
        // 模拟既有库里的 relation_grants JSON: hop 只有 relations/combine/toType/subtree, 无 direction
        String legacy = "{\"chains\":[{\"hops\":[{\"relations\":[\"member\"],\"combine\":\"OR\","
                + "\"toType\":\"org_unit\",\"subtree\":false}],"
                + "\"terminal\":{\"anchorRelations\":[\"owner_org\"],\"combine\":\"OR\"},\"typeFilter\":[]}]}";
        ScopeChainSpec back = om.readValue(legacy, ScopeChainSpec.class);
        assertEquals(Direction.FORWARD, back.chains().get(0).hops().get(0).direction(),
                "无 direction 字段必须默认正向");
    }

    @Test
    @DisplayName("[P-M1] 含 direction=REVERSE 的链 JSON 往返无损")
    void roundTripWithReverseDirection() throws Exception {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(new Hop(List.of("member"), Combine.OR, "user", false, Direction.REVERSE)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ScopeChainSpec back = om.readValue(om.writeValueAsString(spec), ScopeChainSpec.class);
        assertEquals(spec, back);
        assertEquals(Direction.REVERSE, back.chains().get(0).hops().get(0).direction());
    }

    @Test
    @DisplayName("null 字段归一: 缺省 hops/typeFilter → 空列表 (紧凑构造器)")
    void nullsNormalized() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(null, new Terminal(List.of("owner_org"), null), null)));
        Chain c = spec.chains().get(0);
        assertNotNull(c.hops());
        assertTrue(c.hops().isEmpty());
        assertNotNull(c.typeFilter());
        assertEquals(Combine.OR, c.terminal().combine()); // null combine → OR
    }
}
