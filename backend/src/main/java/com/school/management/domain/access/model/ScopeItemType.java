package com.school.management.domain.access.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义范围项类型枚举 (V5)
 * 定义CUSTOM范围下可选择的范围项类型
 */
public enum ScopeItemType {

    ORG_UNIT("ORG_UNIT", "部门", "org_units", "id", "name", "parent_id", true, 1),
    CLASS("CLASS", "班级", "classes", "id", "class_name", null, false, 2),
    GRADE("GRADE", "年级", "grades", "id", "name", null, false, 3),
    BUILDING("BUILDING", "楼栋", "space", "id", "space_name", null, false, 4),
    MAJOR("MAJOR", "专业", "majors", "id", "name", null, false, 5);

    private final String code;
    private final String name;
    private final String refTable;
    private final String refIdField;
    private final String refNameField;
    private final String refParentField;
    private final boolean supportChildren;
    private final int sortOrder;

    ScopeItemType(String code, String name, String refTable, String refIdField,
                  String refNameField, String refParentField, boolean supportChildren, int sortOrder) {
        this.code = code;
        this.name = name;
        this.refTable = refTable;
        this.refIdField = refIdField;
        this.refNameField = refNameField;
        this.refParentField = refParentField;
        this.supportChildren = supportChildren;
        this.sortOrder = sortOrder;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getRefTable() {
        return refTable;
    }

    public String getRefIdField() {
        return refIdField;
    }

    public String getRefNameField() {
        return refNameField;
    }

    public String getRefParentField() {
        return refParentField;
    }

    public boolean isSupportChildren() {
        return supportChildren;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    /**
     * 是否为树形结构（有父级字段）
     */
    public boolean isTreeStructure() {
        return refParentField != null;
    }

    public static ScopeItemType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ScopeItemType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取所有启用的范围项类型
     */
    public static List<ScopeItemType> getAllEnabled() {
        return Arrays.stream(values())
                .sorted((a, b) -> Integer.compare(a.sortOrder, b.sortOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取适用于指定模块的范围项类型
     */
    public static List<ScopeItemType> getApplicableTypes(DataModule module) {
        // 根据模块返回适用的范围项类型
        if (module == null) {
            return getAllEnabled();
        }

        return switch (module) {
            case STUDENT, SCHOOL_CLASS -> Arrays.asList(ORG_UNIT, CLASS, GRADE, MAJOR);
            case DORMITORY_BUILDING, DORMITORY_ROOM -> Arrays.asList(ORG_UNIT, BUILDING);
            case DORMITORY_CHECKIN -> Arrays.asList(ORG_UNIT, CLASS, BUILDING);
            case INSPECTION_TEMPLATE, INSPECTION_PROJECT, INSPECTION_TASK -> Arrays.asList(ORG_UNIT);
            case INSPECTION_RECORD, INSPECTION_SUMMARY, INSPECTION_CORRECTIVE -> Arrays.asList(ORG_UNIT, CLASS);
            case INSPECTION_PERSONAL -> Arrays.asList(ORG_UNIT, CLASS);
            case INSPECTION_APPEAL -> Arrays.asList(ORG_UNIT, CLASS);
            case SYSTEM_USER -> Arrays.asList(ORG_UNIT);
            case SYSTEM_ROLE -> List.of();  // 角色管理不需要范围项
            default -> getAllEnabled();
        };
    }
}
