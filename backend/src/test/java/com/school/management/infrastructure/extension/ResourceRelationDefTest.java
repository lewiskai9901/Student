package com.school.management.infrastructure.extension;

import com.school.management.domain.access.model.Cardinality;
import com.school.management.domain.access.model.StorageKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceRelationDef 构造期校验 —— 统一锚定模型存储/基数自洽规则。
 */
class ResourceRelationDefTest {

    @Test
    @DisplayName("COLUMN 工厂: 单值落列, 字段齐全")
    void validColumnRelationBuilds() {
        ResourceRelationDef d = ResourceRelationDef.column(
                "inspection_record", "creator", "创建者", "USER", "created_by");
        assertEquals(StorageKind.COLUMN, d.storageKind());
        assertEquals(Cardinality.SINGLE, d.cardinality());
        assertEquals("created_by", d.columnName());
        assertFalse(d.autoFill());
        assertFalse(d.grantsByDefault());
    }

    @Test
    @DisplayName("链式修饰: autoFill + grantsByDefault 生效")
    void withersApply() {
        ResourceRelationDef d = ResourceRelationDef
                .column("inspection_record", "owner_org", "所属组织", "ORG_UNIT", "org_unit_id")
                .withAutoFill().withGrantsByDefault();
        assertTrue(d.autoFill());
        assertTrue(d.grantsByDefault());
        assertEquals("org_unit_id", d.columnName());
    }

    @Test
    @DisplayName("§4 列装不下集合: COLUMN 不允许 MULTI 基数")
    void columnCannotBeMulti() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceRelationDef(
                "r", "x", "X", "USER", Cardinality.MULTI, StorageKind.COLUMN,
                "col", null, null, false, false, null, false));
    }

    @Test
    @DisplayName("COLUMN 必须有 columnName")
    void columnRequiresColumnName() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceRelationDef(
                "r", "x", "X", "USER", Cardinality.SINGLE, StorageKind.COLUMN,
                "   ", null, null, false, false, null, false));
    }

    @Test
    @DisplayName("RECORD_RELATION 必须是 MULTI 基数")
    void recordRelationRequiresMulti() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceRelationDef(
                "r", "x", "X", "USER", Cardinality.SINGLE, StorageKind.RECORD_RELATION,
                null, null, "reviewer", false, false, null, false));
    }

    @Test
    @DisplayName("RECORD_RELATION 必须有 arRelation")
    void recordRelationRequiresArRelation() {
        assertThrows(IllegalArgumentException.class,
                () -> ResourceRelationDef.recordRelation("r", "reviewer", "复核员", "USER", null));
    }

    @Test
    @DisplayName("SUBJECT_GRAPH 必须有 arRelation")
    void subjectGraphRequiresArRelation() {
        assertThrows(IllegalArgumentException.class, () -> ResourceRelationDef.subjectGraph(
                "student", "owner_org", "所属组织", "ORG_UNIT", Cardinality.SINGLE, null));
    }

    @Test
    @DisplayName("SUBJECT_GRAPH 允许 MULTI 基数 (如用户管理的多个组织)")
    void subjectGraphAllowsMulti() {
        ResourceRelationDef d = ResourceRelationDef.subjectGraph(
                "user", "managed_orgs", "我管理的组织", "ORG_UNIT", Cardinality.MULTI, "admin");
        assertEquals(StorageKind.SUBJECT_GRAPH, d.storageKind());
        assertEquals(Cardinality.MULTI, d.cardinality());
    }

    @Test
    @DisplayName("MATERIALIZED 不是可声明的关系存储 (它是热路径叠加层)")
    void materializedNotDeclarable() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceRelationDef(
                "r", "x", "X", "USER", Cardinality.SINGLE, StorageKind.MATERIALIZED,
                null, null, null, false, false, null, false));
    }

    @Test
    @DisplayName("空白码被拒绝")
    void blankCodesRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ResourceRelationDef.column("", "creator", "创建者", "USER", "created_by"));
        assertThrows(IllegalArgumentException.class,
                () -> ResourceRelationDef.column("r", "", "创建者", "USER", "created_by"));
        assertThrows(IllegalArgumentException.class,
                () -> ResourceRelationDef.column("r", "creator", "  ", "USER", "created_by"));
        assertThrows(IllegalArgumentException.class,
                () -> ResourceRelationDef.column("r", "creator", "创建者", "", "created_by"));
    }
}
