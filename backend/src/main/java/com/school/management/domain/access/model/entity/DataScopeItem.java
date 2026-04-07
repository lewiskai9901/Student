package com.school.management.domain.access.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 自定义数据范围明细项实体 (V5)
 * 表示CUSTOM范围下的具体范围项配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeItem implements Entity<Long> {

    private Long id;

    /**
     * 关联的角色数据权限ID
     */
    private Long roleDataPermissionId;

    /**
     * 范围项类型代码（ORG_UNIT, CLASS, GRADE, BUILDING, MAJOR）
     */
    private String itemTypeCode;

    /**
     * 范围ID（对应的部门/班级/年级等的实际ID）
     */
    private Long scopeId;

    /**
     * 范围名称（冗余存储，方便展示）
     */
    private String scopeName;

    /**
     * 是否包含子级（仅对树形结构类型有效，如ORG_UNIT）
     */
    @Builder.Default
    private Boolean includeChildren = false;

    private LocalDateTime createdAt;

    /**
     * 判断是否为部门类型
     */
    public boolean isOrgUnitType() {
        return "ORG_UNIT".equals(itemTypeCode);
    }

    /**
     * 判断是否为班级类型
     */
    public boolean isClassType() {
        return "CLASS".equals(itemTypeCode);
    }

    /**
     * 判断是否为年级类型
     */
    public boolean isGradeType() {
        return "GRADE".equals(itemTypeCode);
    }

    /**
     * 创建部门范围项
     */
    public static DataScopeItem createOrgUnit(Long orgUnitId, String orgUnitName, boolean includeChildren) {
        return DataScopeItem.builder()
                .itemTypeCode("ORG_UNIT")
                .scopeId(orgUnitId)
                .scopeName(orgUnitName)
                .includeChildren(includeChildren)
                .build();
    }

    /**
     * 创建班级范围项
     */
    public static DataScopeItem createClass(Long orgUnitId, String className) {
        return DataScopeItem.builder()
                .itemTypeCode("CLASS")
                .scopeId(orgUnitId)
                .scopeName(className)
                .includeChildren(false)
                .build();
    }

    /**
     * 创建年级范围项
     */
    public static DataScopeItem createGrade(Long gradeId, String gradeName) {
        return DataScopeItem.builder()
                .itemTypeCode("GRADE")
                .scopeId(gradeId)
                .scopeName(gradeName)
                .includeChildren(false)
                .build();
    }

    /**
     * 创建楼栋范围项
     */
    public static DataScopeItem createBuilding(Long buildingId, String buildingName) {
        return DataScopeItem.builder()
                .itemTypeCode("BUILDING")
                .scopeId(buildingId)
                .scopeName(buildingName)
                .includeChildren(false)
                .build();
    }
}
