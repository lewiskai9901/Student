package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.chain.Combine;
import com.school.management.domain.access.model.chain.Direction;
import com.school.management.domain.access.model.chain.Hop;
import com.school.management.infrastructure.extension.SqlFragment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChainHopResolver 单测 (统一锚定 P1 Step1): 中间跳 → access_relations 嵌套子查询。
 * 纯 SQL 构造 (不连库), 验结构 + 参数 + AND/OR 组合 + 多级嵌套。
 */
class ChainHopResolverTest {

    private final ChainHopResolver resolver = new ChainHopResolver();

    @Test
    @DisplayName("单跳 OR: 我 →[member]→ 组织 → 一层子查询, 无 GROUP BY")
    void singleOrHop() {
        SqlFragment f = resolver.resolve(
                List.of(new Hop(List.of("member"), Combine.OR, "org_unit", false)), 99L);
        String sql = f.sql();
        assertTrue(sql.contains("FROM access_relations ar0"), sql);
        assertTrue(sql.contains("ar0.subject_type = 'user'"), sql);
        assertTrue(sql.contains("ar0.subject_id = :chmMe"), sql);
        assertTrue(sql.contains("ar0.relation IN (:chmH0r0)"), sql);
        assertTrue(sql.contains("ar0.resource_type = 'org_unit'"), sql);
        assertTrue(sql.contains("ar0.deleted = 0"), sql);
        assertFalse(sql.contains("GROUP BY"), "OR 不应分组: " + sql);
        assertEquals(99L, f.params().get("chmMe"));
        assertEquals("member", f.params().get("chmH0r0"));
    }

    @Test
    @DisplayName("单跳 AND (admin∧responsible_for): GROUP BY HAVING COUNT(DISTINCT)>=2 (交集)")
    void singleAndHop() {
        SqlFragment f = resolver.resolve(
                List.of(new Hop(List.of("admin", "responsible_for"), Combine.AND, "org_unit", true)), 7L);
        String sql = f.sql();
        assertTrue(sql.contains("ar0.relation IN (:chmH0r0,:chmH0r1)"), sql);
        assertTrue(sql.contains("GROUP BY ar0.resource_id"), sql);
        assertTrue(sql.contains("HAVING COUNT(DISTINCT ar0.relation) >= 2"), sql);
        assertEquals("admin", f.params().get("chmH0r0"));
        assertEquals("responsible_for", f.params().get("chmH0r1"));
    }

    @Test
    @DisplayName("AND 单关系不分组 (退化为普通 IN)")
    void andSingleRelationNoGroup() {
        SqlFragment f = resolver.resolve(
                List.of(new Hop(List.of("member"), Combine.AND, "org_unit", false)), 1L);
        assertFalse(f.sql().contains("GROUP BY"), f.sql());
    }

    @Test
    @DisplayName("[#1] 单跳 org 含下级 → tree_path 子树展开 (我管理的组织 + 其后代)")
    void orgHopSubtreeExpands() {
        SqlFragment f = resolver.resolve(
                List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", true)), 9L);
        String sql = f.sql();
        assertTrue(sql.contains("tree_path LIKE CONCAT("), "含下级须 tree_path 子树展开: " + sql);
        assertTrue(sql.contains("FROM org_units"), sql);
        // 内层仍是 admin→org 的 access_relations 子查询 (展开的种子)
        assertTrue(sql.contains("ar0.relation IN (:chmH0r0)"), sql);
        assertEquals("admin", f.params().get("chmH0r0"));
    }

    @Test
    @DisplayName("[#1] 单跳 org 不含下级 → 无 tree_path 展开 (仅直接关系)")
    void orgHopNoSubtreeNoExpand() {
        SqlFragment f = resolver.resolve(
                List.of(new Hop(List.of("admin"), Combine.OR, "org_unit", false)), 9L);
        assertFalse(f.sql().contains("tree_path"), "不含下级不应展开: " + f.sql());
    }

    @Test
    @DisplayName("[#1] place→org 跳含下级 → 投影后再 tree_path 展开 (场所所属组织 + 其后代)")
    void placeToOrgSubtreeExpands() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("manages"), Combine.OR, "place", false),
                new Hop(List.of("belongs_to"), Combine.OR, "org_unit", true)), 5L);
        String sql = f.sql();
        // 外层 = 子树展开, 内含 place→org 投影
        assertTrue(sql.contains("tree_path LIKE CONCAT("), "place→org 含下级也须展开: " + sql);
        assertTrue(sql.contains("effective_org_unit_id"), "展开种子仍是场所投影: " + sql);
    }

    @Test
    @DisplayName("两级链: 我 →[manages]→ 场所 →[belongs_to]→ 组织 → place→org 经 effective_org_unit_id 投影 (P2)")
    void twoLevelChainPlaceProjection() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("manages"), Combine.OR, "place", false),
                new Hop(List.of("belongs_to"), Combine.OR, "org_unit", false)), 5L);
        String sql = f.sql();
        // 外层 = place→org 投影 (places.effective_org_unit_id, 非 access_relations)
        assertTrue(sql.startsWith("SELECT plc1.effective_org_unit_id FROM places plc1"), sql);
        assertTrue(sql.contains("plc1.effective_org_unit_id IS NOT NULL"), sql);
        assertTrue(sql.contains("plc1.id IN (SELECT ar0.resource_id"), "投影 IN 内层 manages→place 子查询: " + sql);
        // 内层 = level0 (manages → place), 起点 :chmMe
        assertTrue(sql.contains("ar0.subject_type = 'user'"), sql);
        assertTrue(sql.contains("ar0.resource_type = 'place'"), sql);
        assertEquals(5L, f.params().get("chmMe"));
        assertEquals("manages", f.params().get("chmH0r0"));
        // belongs_to 关系码不入投影 SQL (投影即真相), 故无 chmH1r0
        assertFalse(sql.contains("chmH1r0"), "place→org 投影不绑 belongs_to 关系参数: " + sql);
    }

    @Test
    @DisplayName("三级链 (上限) 正常嵌套")
    void threeLevelChain() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("supervisor_of"), Combine.OR, "user", false),
                new Hop(List.of("member"), Combine.OR, "org_unit", false),
                new Hop(List.of("belongs_to"), Combine.OR, "org_unit", false)), 3L);
        // 最外 level2, 含两层嵌套
        assertTrue(f.sql().startsWith("SELECT ar2.resource_id"), f.sql());
        assertTrue(f.sql().contains("ar1.subject_id IN (SELECT ar0.resource_id"), f.sql());
        assertTrue(f.sql().contains("ar2.subject_id IN (SELECT ar1.resource_id"), f.sql());
    }

    @Test
    @DisplayName("[审计#3] place→org 跳关系为空 → 不抛 (走投影, 忽略关系)")
    void placeToOrgEmptyRelationsResolves() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("manages"), Combine.OR, "place", false),
                new Hop(List.of(), Combine.OR, "org_unit", false)), 5L);
        assertTrue(f.sql().contains("effective_org_unit_id"), f.sql());
    }

    @Test
    @DisplayName("[P-E1] 反向走跳 level1: 我[管理]组织 →[反·成员]用户 (组织→其成员用户)")
    void reverseHopAfterForward() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("admin"), Combine.OR, "org_unit", false, Direction.FORWARD),
                new Hop(List.of("member"), Combine.OR, "user", false, Direction.REVERSE)), 9L);
        String sql = f.sql();
        // 末跳反向: 选 subject_id, 匹配 resource 侧 = 上一级(org)集
        assertTrue(sql.startsWith("SELECT ar1.subject_id FROM access_relations ar1"), sql);
        assertTrue(sql.contains("ar1.resource_type = 'org_unit'"), sql);   // 匹配侧 = prev
        assertTrue(sql.contains("ar1.resource_id IN (SELECT ar0.resource_id"), sql); // 内层 forward
        assertTrue(sql.contains("ar1.relation IN (:chmH1r0)"), sql);
        assertTrue(sql.contains("ar1.subject_type = 'user'"), sql);        // 选取侧 = toType
        assertEquals("member", f.params().get("chmH1r0"));
    }

    @Test
    @DisplayName("[P-E1] 反向走跳 level0: 我[反·上级] → 我的上级们 (supervisor_of 倒读)")
    void reverseHopLevel0() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("supervisor_of"), Combine.OR, "user", false, Direction.REVERSE)), 9L);
        String sql = f.sql();
        assertTrue(sql.startsWith("SELECT ar0.subject_id FROM access_relations ar0"), sql);
        assertTrue(sql.contains("ar0.resource_type = 'user'"), sql);   // 匹配侧 = 起点 user
        assertTrue(sql.contains("ar0.resource_id = :chmMe"), sql);     // 我作为 resource
        assertTrue(sql.contains("ar0.subject_type = 'user'"), sql);    // 选取侧 = toType
        assertEquals(9L, f.params().get("chmMe"));
    }

    @Test
    @DisplayName("[P-E1] 反向投影 org→place: 我[管理]组织 →[反·归属]场所 (组织下辖场所)")
    void reverseProjectionOrgToPlace() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("admin"), Combine.OR, "org_unit", false, Direction.FORWARD),
                new Hop(List.of("belongs_to"), Combine.OR, "place", false, Direction.REVERSE)), 9L);
        String sql = f.sql();
        // org→place 反向投影: 找 effective_org_unit_id ∈ 上一级组织集 的场所
        assertTrue(sql.startsWith("SELECT plc1.id FROM places plc1"), sql);
        assertTrue(sql.contains("plc1.effective_org_unit_id IN (SELECT ar0.resource_id"), sql);
        assertTrue(sql.contains("plc1.deleted = 0"), sql);
    }

    @Test
    @DisplayName("[P-E1] 反向 AND 多关系: GROUP BY 选取侧 subject_id (交集)")
    void reverseAndGroupBySelectSide() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("admin"), Combine.OR, "org_unit", false, Direction.FORWARD),
                new Hop(List.of("member", "responsible_for"), Combine.AND, "user", false, Direction.REVERSE)), 9L);
        String sql = f.sql();
        assertTrue(sql.contains("GROUP BY ar1.subject_id"), "反向 AND 须按选取侧 subject_id 分组: " + sql);
        assertTrue(sql.contains("HAVING COUNT(DISTINCT ar1.relation) >= 2"), sql);
    }

    @Test
    @DisplayName("空 hops → 抛 (终端直接绑用户, 不需中间跳)")
    void emptyHopsThrows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(List.of(), 1L));
    }

    @Test
    @DisplayName("非法 toType → 抛 (防注入)")
    void badToTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(
                List.of(new Hop(List.of("x"), Combine.OR, "department", false)), 1L));
    }
}
