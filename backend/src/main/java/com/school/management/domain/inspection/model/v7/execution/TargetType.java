package com.school.management.domain.inspection.model.v7.execution;

/**
 * V7 检查目标类型 — 通用核心,不绑定任何行业.
 *
 * 历史上曾有 CLASS 枚举值(教育特定),但班级本质是 ORG 的一种子类型
 * (org_unit.unitType=CLASS),通用核心不应认识"班级"概念.
 * 要区分班级/年级/部门,在业务代码里查 entity_type_configs.typeCode 即可.
 */
public enum TargetType {
    ORG,       // 组织(含班级/年级/部门等,由 orgUnit.unitType 区分)
    PLACE,     // 场所
    USER,      // 用户(个人)
    ASSET,     // 资产
    COMPOSITE; // 复合目标
}
