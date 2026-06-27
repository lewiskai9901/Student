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
                new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id", null, null),
                new AnchorRow("creator", StorageKind.COLUMN, "created_by", null, null)));
        assertFalse(d.viaMembership());
        assertEquals("org_unit_id", d.orgUnitField());
        assertEquals("created_by", d.creatorField());
    }

    @Test
    @DisplayName("成员主体 owner_org(SUBJECT_GRAPH) → viaMembership, orgUnitField=null")
    void subjectGraphImpliesMembership() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.SUBJECT_GRAPH, null, null, null)));
        assertTrue(d.viaMembership());
        assertNull(d.orgUnitField());
        assertNull(d.creatorField());
        assertNull(d.membershipSubjectColumn());  // 未声明 → null (buildMeta 兜底 "id")
    }

    @Test
    @DisplayName("[Tier2] owner_org(SUBJECT_GRAPH) 带 subject_column → membershipSubjectColumn 归注册表")
    void subjectGraphCarriesSubjectColumn() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.SUBJECT_GRAPH, null, null, "user_id")));
        assertTrue(d.viaMembership());
        assertEquals("user_id", d.membershipSubjectColumn());  // user_student: 主表是档案, subject=user_id
    }

    @Test
    @DisplayName("仅 creator(无 org 维度, 如 exam_batch) → orgUnitField=null, 非 membership")
    void creatorOnly() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("creator", StorageKind.COLUMN, "created_by", null, null)));
        assertFalse(d.viaMembership());
        assertNull(d.orgUnitField());
        assertEquals("created_by", d.creatorField());
    }

    @Test
    @DisplayName("owner_org 列名随业务 (org_unit=parent_id 冻结现状)")
    void ownerOrgCustomColumn() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.COLUMN, "parent_id", null, null),
                new AnchorRow("creator", StorageKind.COLUMN, "created_by", null, null)));
        assertFalse(d.viaMembership());
        assertEquals("parent_id", d.orgUnitField());
    }

    @Test
    @DisplayName("creator 列名随业务 (recorded_by / student_id / teacher_id)")
    void creatorCustomColumn() {
        DerivedAnchor d = ResourceRelationRegistry.deriveAnchor(List.of(
                new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id", null, null),
                new AnchorRow("creator", StorageKind.COLUMN, "recorded_by", null, null)));
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
                        new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id", null, null))),
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
                        new AnchorRow("creator", StorageKind.COLUMN, "created_by", null, null))),
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

    // ── R4: relationOf per-relation storage_kind 查询 (RECORD_RELATION 检测前置) ──

    @Test
    @DisplayName("relationOf: 暴露任意关系的 storage_kind (reviewer=RECORD_RELATION); 未注册→empty")
    void relationOf_exposesPerRelationStorageKind() {
        AtomicInteger fetches = new AtomicInteger();
        ResourceRelationRegistry reg = registryReturning(
                Map.of("inspection_record", List.of(
                        new AnchorRow("owner_org", StorageKind.COLUMN, "org_unit_id", null, null),
                        new AnchorRow("creator", StorageKind.COLUMN, "created_by", null, null),
                        new AnchorRow("reviewer", StorageKind.RECORD_RELATION, null, null, null))),
                fetches);

        assertEquals(StorageKind.RECORD_RELATION,
                reg.relationOf("inspection_record", "reviewer").orElseThrow().storageKind(),
                "relationOf 须暴露 reviewer=RECORD_RELATION (forResource 的 DerivedAnchor 不含此关系)");
        assertEquals(StorageKind.COLUMN,
                reg.relationOf("inspection_record", "owner_org").orElseThrow().storageKind());
        assertTrue(reg.relationOf("inspection_record", "nonexistent").isEmpty());
        assertTrue(reg.relationOf("unregistered_res", "reviewer").isEmpty());
        assertEquals(1, fetches.get(), "relationOf 与 forResource 共享同一懒加载");
    }

    // ── R3c 守护: providerResolverBeans 列出所有 PROVIDER 关系的 resolver bean ──

    @Test
    @DisplayName("providerResolverBeans: 仅收集 PROVIDER 行的 resource/relation → bean (COLUMN/RECORD_RELATION 排除)")
    void providerResolverBeans_collectsOnlyProviderRows() {
        AtomicInteger fetches = new AtomicInteger();
        ResourceRelationRegistry reg = registryReturning(
                Map.of(
                    "student", List.of(
                        new AnchorRow("owner_org", StorageKind.SUBJECT_GRAPH, null, null, null),
                        new AnchorRow("taught_by", StorageKind.PROVIDER, null, "teachingStudentResolver", null)),
                    "inspection_record", List.of(
                        new AnchorRow("creator", StorageKind.COLUMN, "created_by", null, null),
                        new AnchorRow("reviewer", StorageKind.RECORD_RELATION, null, null, null))),
                fetches);

        Map<String, String> beans = reg.providerResolverBeans();

        assertEquals(1, beans.size(), "仅 1 个 PROVIDER 行 (taught_by); 其余存储种类排除");
        assertEquals("teachingStudentResolver", beans.get("student/taught_by"));
    }
}
