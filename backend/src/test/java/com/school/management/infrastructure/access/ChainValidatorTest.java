package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import com.school.management.domain.access.model.chain.Chain;
import com.school.management.domain.access.model.chain.Combine;
import com.school.management.domain.access.model.chain.Direction;
import com.school.management.domain.access.model.chain.Hop;
import com.school.management.domain.access.model.chain.ScopeChainSpec;
import com.school.management.domain.access.model.chain.Terminal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChainValidator 单测 (统一锚定 P0): 终端∈注册表 / 限深 / 结构。
 *
 * <p>用注册表 seam (覆写 fetchByResource) 喂 canned 锚点, 免 DB —— student 注册
 * owner_org(成员图)/taught_by(PROVIDER)/creator(列)。
 */
class ChainValidatorTest {

    /** canned relation_types 边 (code|from|to) 供方向校验。 */
    private static final Set<String> EDGES = Set.of(
            "admin|user|org_unit", "member|user|org_unit", "responsible_for|user|org_unit",
            "manages|user|place", "belongs_to|place|org_unit", "supervisor_of|user|user");

    private ChainValidator validator() {
        ResourceRelationRegistry reg = new ResourceRelationRegistry(null) {
            @Override
            protected Map<String, List<AnchorRow>> fetchByResource() {
                Map<String, List<AnchorRow>> m = new HashMap<>();
                m.put("student", List.of(
                        new AnchorRow("owner_org", StorageKind.SUBJECT_GRAPH, null, null),
                        new AnchorRow("taught_by", StorageKind.PROVIDER, null, "teachingStudentResolver"),
                        new AnchorRow("creator", StorageKind.COLUMN, "created_by", null)));
                return m;
            }
            @Override
            protected Set<String> fetchInsertGuardedResources() {
                return Set.of();
            }
        };
        com.school.management.application.access.RelationTypeRegistry rtr =
                new com.school.management.application.access.RelationTypeRegistry(null) {
                    @Override
                    public boolean isRegistered(String code, String fromType, String toType) {
                        return EDGES.contains(code + "|" + fromType + "|" + toType);
                    }
                };
        return new ChainValidator(reg, rtr);
    }

    @Test
    @DisplayName("用户原例: 我[管理且负责]组织(含下级) → 终端 owner_org + 类型 → 合法")
    void validUserExample() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(new Hop(List.of("admin", "responsible_for"), Combine.AND, "org_unit", true)),
                        new Terminal(List.of("owner_org"), Combine.OR),
                        List.of("STUDENT"))));
        ChainValidator.Result r = validator().validate("student", spec);
        assertTrue(r.valid(), () -> "应合法, errors=" + r.errors());
    }

    @Test
    @DisplayName("1 跳退化 (无中间跳 + 单终端) = 旧 grant → 合法")
    void oneHopDegenerate() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        assertTrue(validator().validate("student", spec).valid());
    }

    @Test
    @DisplayName("多终端锚点 (owner_org ∧ taught_by) 皆已注册 → 合法")
    void multiRegisteredTerminal() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("owner_org", "taught_by"), Combine.AND), List.of())));
        assertTrue(validator().validate("student", spec).valid());
    }

    @Test
    @DisplayName("终端锚点未注册 (member 不在 student 的 resource_relations) → 拒绝 + 提示可选")
    void unregisteredTerminalRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("member"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("member") && e.contains("未在资源")),
                () -> "应提示 member 未注册, errors=" + r.errors());
    }

    @Test
    @DisplayName("超限深 (4 跳 > MAX_DEPTH 3) → 拒绝")
    void depthExceededRejected() {
        Hop h = new Hop(List.of("member"), Combine.OR, "org_unit", false);
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(h, h, h, h), new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("限深")));
    }

    @Test
    @DisplayName("跳到达类型非法 (department 非三大主体) → 拒绝")
    void badToTypeRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(new Hop(List.of("member"), Combine.OR, "department", false)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("类型非法")));
    }

    @Test
    @DisplayName("PROVIDER 终端 + 中间跳 → 拒绝 (此类终端不消费组织集, 须 hops 空)")
    void providerTerminalWithHopsRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", false)),
                        new Terminal(List.of("taught_by"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("taught_by") && e.contains("不支持中间跳")),
                () -> r.errors().toString());
    }

    @Test
    @DisplayName("PROVIDER 终端 + 空跳 → 合法 (走旧路径)")
    void providerTerminalNoHopsOk() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("taught_by"), Combine.OR), List.of())));
        assertTrue(validator().validate("student", spec).valid());
    }

    @Test
    @DisplayName("[审计#2] creator 终端 + 中间跳 → 拒绝 (creator 绑当前用户, 中间跳会被忽略)")
    void creatorTerminalWithHopsRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", false)),
                        new Terminal(List.of("creator"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("creator") && e.contains("忽略")),
                () -> r.errors().toString());
    }

    @Test
    @DisplayName("[审计#2] creator 终端 + 空跳 → 合法 (我创建的, 退化路径)")
    void creatorTerminalNoHopsOk() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(), new Terminal(List.of("creator"), Combine.OR), List.of())));
        assertTrue(validator().validate("student", spec).valid());
    }

    @Test
    @DisplayName("[审计#2] creator + owner_org 混用 + 中间跳 → 合法 (owner_org 消费跳, creator 加用户约束)")
    void creatorMixedWithOrgAnchorWithHopsOk() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", false)),
                        new Terminal(List.of("owner_org", "creator"), Combine.AND), List.of())));
        assertTrue(validator().validate("student", spec).valid(),
                () -> validator().validate("student", spec).errors().toString());
    }

    @Test
    @DisplayName("[P-V1] 反向走跳 + 边存在: 我[admin]组织→[反·member]用户 → 合法 (member 反向 = user→org 边)")
    void reverseEdgeValid() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(
                                new Hop(List.of("admin"), Combine.OR, "org_unit", false, Direction.FORWARD),
                                new Hop(List.of("member"), Combine.OR, "user", false, Direction.REVERSE)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertTrue(r.valid(), () -> "反向 member 边应合法, errors=" + r.errors());
    }

    @Test
    @DisplayName("[P-V1] 方向反了 → 拒绝: member 正向从 org→user (member 实为 user→org)")
    void forwardWrongDirectionRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(
                                new Hop(List.of("admin"), Combine.OR, "org_unit", false, Direction.FORWARD),
                                new Hop(List.of("member"), Combine.OR, "user", false, Direction.FORWARD)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("member") && e.contains("org_unit→user")),
                () -> r.errors().toString());
    }

    @Test
    @DisplayName("[P-V1/审计#4] 乱填关系码 → 拒绝 + 友好报错 (relation_types 无此边)")
    void bogusRelationRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(new Hop(List.of("gibberish"), Combine.OR, "org_unit", false, Direction.FORWARD)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("gibberish") && e.contains("不存在")),
                () -> r.errors().toString());
    }

    @Test
    @DisplayName("空链 → 拒绝")
    void emptyRejected() {
        assertFalse(validator().validate("student", new ScopeChainSpec(List.of())).valid());
        assertFalse(validator().validate("student", null).valid());
    }

    @Test
    @DisplayName("跳关系为空 → 拒绝")
    void emptyHopRelationsRejected() {
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(List.of(new Hop(List.of(), Combine.OR, "org_unit", false)),
                        new Terminal(List.of("owner_org"), Combine.OR), List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertFalse(r.valid());
        assertTrue(r.errors().stream().anyMatch(e -> e.contains("关系为空")));
    }

    @Test
    @DisplayName("[审计#3] place→org 跳关系为空 → 合法 (引擎走 effective_org_unit_id 投影, 忽略关系)")
    void placeToOrgEmptyRelationsOk() {
        // UI 隐藏了 place→org 跳的关系选择 → relations 为空; 引擎按场所归属投影, 不应判"关系为空"
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(
                                new Hop(List.of("manages"), Combine.OR, "place", false),
                                new Hop(List.of(), Combine.OR, "org_unit", false)),
                        new Terminal(List.of("owner_org"), Combine.OR),
                        List.of())));
        ChainValidator.Result r = validator().validate("student", spec);
        assertTrue(r.valid(), () -> "place→org 空关系应合法, errors=" + r.errors());
    }

    @Test
    @DisplayName("经场所链: 我[管理]场所→[占用]... 多级 (3 跳=上限) 终端合法 → 合法")
    void multiLevelPlaceChain() {
        // 我 --manages--> place --(占用)--> ... 终端 owner_org (3 跳上限内)
        ScopeChainSpec spec = new ScopeChainSpec(List.of(
                new Chain(
                        List.of(
                                new Hop(List.of("manages"), Combine.OR, "place", false),
                                new Hop(List.of("belongs_to"), Combine.OR, "org_unit", true)),
                        new Terminal(List.of("owner_org"), Combine.OR),
                        List.of())));
        assertTrue(validator().validate("student", spec).valid(),
                () -> validator().validate("student", spec).errors().toString());
    }
}
