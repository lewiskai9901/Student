package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import com.school.management.domain.access.model.chain.Chain;
import com.school.management.domain.access.model.chain.Combine;
import com.school.management.domain.access.model.chain.Hop;
import com.school.management.domain.access.model.chain.Terminal;
import com.school.management.infrastructure.extension.SqlFragment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChainCompiler 单测 (统一锚定 P1 Step2a/2b/2c): 链 → 资源谓词。终端列取自 ResourceScopeMeta
 * (owner_org→orgUnitField / creator→creatorField / SUBJECT_GRAPH→membershipSubjectColumn), 使 READ 用真列、
 * INSERT 合成 org 元 (orgUnitField=id) 探 org_units 两用。纯 SQL 构造 (注册表 seam 喂 canned 锚点)。
 */
class ChainCompilerTest {

    private static final long T = 1L; // tenant
    // 列锚资源 (别名 t. / 空)
    private static final ResourceScopeMeta DOC_T = new ResourceScopeMeta("t", "org_unit_id", "created_by", false, "user_id", null, "doc");
    private static final ResourceScopeMeta DOC = new ResourceScopeMeta("", "org_unit_id", "created_by", false, "user_id", null, "doc");
    // 成员图资源
    private static final ResourceScopeMeta STUDENT_S = new ResourceScopeMeta("s", null, null, true, "user_id", null, "student");
    private static final ResourceScopeMeta STUDENT = new ResourceScopeMeta("", null, null, true, "user_id", null, "student");

    private ChainCompiler compiler() {
        ResourceRelationRegistry reg = new ResourceRelationRegistry(null) {
            @Override
            protected Map<String, List<AnchorRow>> fetchByResource() {
                Map<String, List<AnchorRow>> m = new HashMap<>();
                m.put("doc", List.of(   // 列锚资源
                        new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id", null),
                        new AnchorRow("creator", StorageKind.COLUMN, "created_by", null)));
                m.put("student", List.of(   // 成员图资源
                        new AnchorRow("owner_org", StorageKind.SUBJECT_GRAPH, null, null),
                        new AnchorRow("taught_by", StorageKind.PROVIDER, null, "teachingStudentResolver")));
                return m;
            }
            @Override
            protected Set<String> fetchInsertGuardedResources() { return Set.of(); }
        };
        return new ChainCompiler(new ChainHopResolver(), reg);
    }

    @Test
    @DisplayName("1 跳 + COLUMN owner_org: 我[member]组织 → org_unit_id IN (跳子查询)")
    void columnOwnerOrgOneHop() {
        Chain c = new Chain(
                List.of(new Hop(List.of("member"), Combine.OR, "org_unit", false)),
                new Terminal(List.of("owner_org"), Combine.OR), List.of());
        SqlFragment f = compiler().compileChain(c, DOC_T, 9L, T);
        assertTrue(f.sql().startsWith("t.org_unit_id IN (SELECT ar0.resource_id"), f.sql());
        assertEquals(9L, f.params().get("chmMe"));
        assertEquals("member", f.params().get("chmH0r0"));
    }

    @Test
    @DisplayName("creator + 空 hops → created_by = :me (绑用户, 无中间跳)")
    void creatorBindsUser() {
        Chain c = new Chain(List.of(), new Terminal(List.of("creator"), Combine.OR), List.of());
        SqlFragment f = compiler().compileChain(c, DOC, 42L, T);
        assertEquals("created_by = :ccMe0", f.sql());
        assertEquals(42L, f.params().get("ccMe0"));
    }

    @Test
    @DisplayName("INSERT 探针: 合成 org 元 (orgUnitField=id) → owner_org 终端产 id IN (S) 探 org_units")
    void insertOrgProbeUsesMetaColumn() {
        ResourceScopeMeta orgProbe = new ResourceScopeMeta("", "id", "", false, "id", null, "doc");
        Chain c = new Chain(
                List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", false)),
                new Terminal(List.of("owner_org"), Combine.OR), List.of());
        SqlFragment f = compiler().compileChain(c, orgProbe, 7L, T);
        assertTrue(f.sql().startsWith("id IN (SELECT ar0.resource_id"), "INSERT 探针应产 id IN(S): " + f.sql());
    }

    @Test
    @DisplayName("SUBJECT_GRAPH 终端 + 多跳: 数据.user_id ∈ S 各组织的 member (含 tenant 过滤)")
    void subjectGraphTerminal() {
        Chain c = new Chain(
                List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", false)),
                new Terminal(List.of("owner_org"), Combine.OR), List.of());
        SqlFragment f = compiler().compileChain(c, STUDENT_S, 1L, T);
        String sql = f.sql();
        assertTrue(sql.startsWith("s.user_id IN (SELECT ccm0.subject_id FROM access_relations ccm0"), sql);
        assertTrue(sql.contains("ccm0.relation = 'member'"), sql);
        assertTrue(sql.contains("ccm0.tenant_id = :ccTenant0"), sql);
        assertTrue(sql.contains("ccm0.resource_id IN (SELECT ar0.resource_id"), "应嵌入跳子查询: " + sql);
        assertEquals(1L, f.params().get("ccTenant0"));
    }

    @Test
    @DisplayName("多终端 AND (owner_org ∧ creator) → 两谓词 AND")
    void multiAnchorAnd() {
        Chain c = new Chain(
                List.of(new Hop(List.of("member"), Combine.OR, "org_unit", false)),
                new Terminal(List.of("owner_org", "creator"), Combine.AND), List.of());
        SqlFragment f = compiler().compileChain(c, DOC, 5L, T);
        assertTrue(f.sql().startsWith("("), f.sql());
        assertTrue(f.sql().contains(" AND "), f.sql());
        assertTrue(f.sql().contains("org_unit_id IN"), f.sql());
        assertTrue(f.sql().contains("created_by = :ccMe1"), f.sql()); // creator 锚点 index 1
    }

    @Test
    @DisplayName("两级链 (我 manages 场所 → belongs_to 组织) + owner_org 终端")
    void twoLevelChainColumn() {
        Chain c = new Chain(
                List.of(
                        new Hop(List.of("manages"), Combine.OR, "place", false),
                        new Hop(List.of("belongs_to"), Combine.OR, "org_unit", false)),
                new Terminal(List.of("owner_org"), Combine.OR), List.of());
        SqlFragment f = compiler().compileChain(c, DOC, 3L, T);
        assertTrue(f.sql().startsWith("org_unit_id IN (SELECT ar1.resource_id"), f.sql());
        assertTrue(f.sql().contains("ar1.subject_id IN (SELECT ar0.resource_id"), f.sql());
    }

    @Test
    @DisplayName("未注册终端 → DENY (fail-closed)")
    void unregisteredTerminalDeny() {
        Chain c = new Chain(List.of(), new Terminal(List.of("bogus"), Combine.OR), List.of());
        assertEquals("1=0", compiler().compileChain(c, DOC, 1L, T).sql());
    }

    @Test
    @DisplayName("PROVIDER 终端 (链路径) → fail-closed DENY (不崩; 此类终端须 hops 空走旧路径)")
    void providerFailClosedInChain() {
        Chain c = new Chain(List.of(), new Terminal(List.of("taught_by"), Combine.OR), List.of());
        assertEquals("1=0", compiler().compileChain(c, STUDENT, 1L, T).sql());
    }
}
