package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.chain.Combine;
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
    @DisplayName("两级链: 我 →[manages]→ 场所 →[belongs_to]→ 组织 → place→org 经 effective_org_unit_id 投影 (P2)")
    void twoLevelChainPlaceProjection() {
        SqlFragment f = resolver.resolve(List.of(
                new Hop(List.of("manages"), Combine.OR, "place", false),
                new Hop(List.of("belongs_to"), Combine.OR, "org_unit", true)), 5L);
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
