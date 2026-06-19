package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import com.school.management.infrastructure.access.ResourceRelationRegistry.AnchorRow;
import com.school.management.infrastructure.access.ResourceRelationRegistry.DerivedAnchor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceRelationRegistry.deriveAnchor 纯逻辑单测 (R2.2): 注册行 → 派生锚点。
 * 派生结果须与现有 buildMeta 的有效值等价 (orgUnitField/creatorField/viaMembership)。
 */
class ResourceRelationRegistryTest {

    @Test
    @DisplayName("普通记录 owner_org(列) + creator(列) → 列锚, 非 membership")
    void plainColumnAnchors() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id"),
                new AnchorRow("creator", StorageKind.COLUMN, "created_by")));
        assertFalse(d.viaMembership());
        assertEquals("org_unit_id", d.orgUnitField());
        assertEquals("created_by", d.creatorField());
    }

    @Test
    @DisplayName("成员主体 owner_org(SUBJECT_GRAPH) → viaMembership, orgUnitField=null")
    void subjectGraphImpliesMembership() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.SUBJECT_GRAPH, null)));
        assertTrue(d.viaMembership());
        assertNull(d.orgUnitField());
        assertNull(d.creatorField());
    }

    @Test
    @DisplayName("仅 creator(无 org 维度, 如 exam_batch) → orgUnitField=null, 非 membership")
    void creatorOnly() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("creator", StorageKind.COLUMN, "created_by")));
        assertFalse(d.viaMembership());
        assertNull(d.orgUnitField());
        assertEquals("created_by", d.creatorField());
    }

    @Test
    @DisplayName("owner_org 列名随业务 (org_unit=parent_id 冻结现状)")
    void ownerOrgCustomColumn() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.COLUMN, "parent_id"),
                new AnchorRow("creator", StorageKind.COLUMN, "created_by")));
        assertFalse(d.viaMembership());
        assertEquals("parent_id", d.orgUnitField());
    }

    @Test
    @DisplayName("creator 列名随业务 (recorded_by / student_id / teacher_id)")
    void creatorCustomColumn() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id"),
                new AnchorRow("creator", StorageKind.COLUMN, "recorded_by")));
        assertEquals("recorded_by", d.creatorField());
    }

    @Test
    @DisplayName("空注册行 → 全 null/false (无锚, buildMeta 走兜底)")
    void emptyRows() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of());
        assertFalse(d.viaMembership());
        assertNull(d.orgUnitField());
        assertNull(d.creatorField());
    }

    // ── P3 健壮性: 懒加载 + load-once + 空注册表 fail-fast ──────────────────────
    // 用覆写 fetchByResource() 的测试子类喂 canned 行 (免 ResultSet mock / 免真 DB)。

    /** 测试 seam: 覆写 fetchByResource 返回 canned 行并计数, jdbc 不被使用 (传 null)。 */
    private static ResourceRelationRegistry registryReturning(
            Map<String, List<AnchorRow>> rows, AtomicInteger fetchCount) {
        return new ResourceRelationRegistry(null) {
            @Override
            protected Map<String, List<AnchorRow>> fetchByResource() {
                fetchCount.incrementAndGet();
                return rows;
            }
        };
    }

    @Test
    @DisplayName("懒加载: forResource 首次访问自行触发加载 (无需显式 load/run — 消除启动窗口)")
    void forResource_lazilyLoadsOnFirstAccess() {
        AtomicInteger fetches = new AtomicInteger();
        ResourceRelationRegistry reg = registryReturning(
                Map.of("inspection_record", List.of(
                        new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id"))),
                fetches);

        // 未调用 load()/run(); 直接 forResource 应自行加载
        Optional<DerivedAnchor> d = reg.forResource("inspection_record");

        assertTrue(d.isPresent(), "懒加载未触发: forResource 应在缓存空时自行加载");
        assertEquals("org_unit_id", d.get().orgUnitField());
        assertEquals(1, fetches.get(), "首次 forResource 应触发恰好一次加载");
    }

    @Test
    @DisplayName("load-once: 多次 forResource 只加载一次 (缓存复用, 非每次查库)")
    void forResource_loadsOnlyOnce() {
        AtomicInteger fetches = new AtomicInteger();
        ResourceRelationRegistry reg = registryReturning(
                Map.of("inspection_record", List.of(
                        new AnchorRow("creator", StorageKind.COLUMN, "created_by"))),
                fetches);

        reg.forResource("inspection_record");
        reg.forResource("inspection_record");
        reg.forResource("other");

        assertEquals(1, fetches.get(), "加载应只发生一次, 后续走缓存");
    }

    @Test
    @DisplayName("fail-fast: 注册表为空 (contribution 写入失败) → forResource 抛 IllegalStateException, 不静默 fail-open")
    void emptyRegistry_failsFast() {
        AtomicInteger fetches = new AtomicInteger();
        ResourceRelationRegistry reg = registryReturning(new HashMap<>(), fetches);

        assertThrows(IllegalStateException.class, () -> reg.forResource("anything"),
                "空注册表删兜底后无锚点 → 必须 fail-fast, 不得静默放行");
    }
}
