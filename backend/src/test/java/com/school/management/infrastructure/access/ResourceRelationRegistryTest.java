package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.StorageKind;
import com.school.management.infrastructure.access.ResourceRelationRegistry.AnchorRow;
import com.school.management.infrastructure.access.ResourceRelationRegistry.DerivedAnchor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
