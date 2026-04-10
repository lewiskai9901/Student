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
     * 判断是否为指定类型
     */
    public boolean isType(String typeCode) {
        return typeCode != null && typeCode.equals(itemTypeCode);
    }

    /**
     * 创建组织范围项
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
     * 创建通用范围项（按类型编码）
     */
    public static DataScopeItem create(String typeCode, Long scopeId, String scopeName) {
        return DataScopeItem.builder()
                .itemTypeCode(typeCode)
                .scopeId(scopeId)
                .scopeName(scopeName)
                .includeChildren(false)
                .build();
    }
}
