package com.school.management.domain.space.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用空间类型实体
 * 支持完全配置化的空间类型定义，适用于学校、医院、公司等各类场景
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalSpaceType implements Entity<Long> {

    private Long id;

    // ==================== 基础信息 ====================

    /**
     * 类型编码（唯一标识）
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序号
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 是否系统预置（系统预置的不可删除）
     */
    @Builder.Default
    private boolean isSystem = false;

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean isEnabled = true;

    // ==================== 层级关系 ====================

    /**
     * 允许的子类型编码列表
     */
    @Builder.Default
    private List<String> allowedChildTypes = new ArrayList<>();

    /**
     * 是否可作为根节点
     */
    @Builder.Default
    private boolean isRootType = false;

    // ==================== 行为特性 ====================

    /**
     * 是否有容量概念
     */
    @Builder.Default
    private boolean hasCapacity = false;

    /**
     * 是否可预订
     */
    @Builder.Default
    private boolean bookable = false;

    /**
     * 是否可分配给组织
     */
    @Builder.Default
    private boolean assignable = false;

    /**
     * 是否可入住/占用
     */
    @Builder.Default
    private boolean occupiable = false;

    // ==================== 容量配置 ====================

    /**
     * 容量单位（人/床位/工位/平方米）
     */
    private String capacityUnit;

    /**
     * 默认容量
     */
    private Integer defaultCapacity;

    // ==================== 扩展属性 ====================

    /**
     * 扩展属性JSON Schema
     */
    private String attributeSchema;

    // ==================== 业务方法 ====================

    /**
     * 判断是否允许某类型作为子类型
     */
    public boolean canHaveChild(String childTypeCode) {
        return allowedChildTypes != null && allowedChildTypes.contains(childTypeCode);
    }

    /**
     * 判断是否为叶子类型（不能有子节点）
     */
    public boolean isLeafType() {
        return allowedChildTypes == null || allowedChildTypes.isEmpty();
    }

    /**
     * 添加允许的子类型
     */
    public void addAllowedChildType(String childTypeCode) {
        if (allowedChildTypes == null) {
            allowedChildTypes = new ArrayList<>();
        }
        if (!allowedChildTypes.contains(childTypeCode)) {
            allowedChildTypes.add(childTypeCode);
        }
    }

    /**
     * 移除允许的子类型
     */
    public void removeAllowedChildType(String childTypeCode) {
        if (allowedChildTypes != null) {
            allowedChildTypes.remove(childTypeCode);
        }
    }

    /**
     * 启用
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * 禁用
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * 验证配置的一致性
     */
    public void validate() {
        if (typeCode == null || typeCode.isBlank()) {
            throw new IllegalArgumentException("类型编码不能为空");
        }
        if (typeName == null || typeName.isBlank()) {
            throw new IllegalArgumentException("类型名称不能为空");
        }
        // 如果有容量，则必须有容量单位
        if (hasCapacity && (capacityUnit == null || capacityUnit.isBlank())) {
            throw new IllegalArgumentException("有容量的类型必须指定容量单位");
        }
        // 可占用的类型必须有容量
        if (occupiable && !hasCapacity) {
            throw new IllegalArgumentException("可占用的类型必须有容量概念");
        }
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建根类型
     */
    public static UniversalSpaceType createRootType(String typeCode, String typeName, List<String> allowedChildTypes) {
        return UniversalSpaceType.builder()
                .typeCode(typeCode)
                .typeName(typeName)
                .isRootType(true)
                .allowedChildTypes(allowedChildTypes != null ? allowedChildTypes : new ArrayList<>())
                .build();
    }

    /**
     * 创建中间层类型（可有子节点）
     */
    public static UniversalSpaceType createIntermediateType(String typeCode, String typeName, List<String> allowedChildTypes) {
        return UniversalSpaceType.builder()
                .typeCode(typeCode)
                .typeName(typeName)
                .isRootType(false)
                .allowedChildTypes(allowedChildTypes != null ? allowedChildTypes : new ArrayList<>())
                .build();
    }

    /**
     * 创建叶子类型（不能有子节点）
     */
    public static UniversalSpaceType createLeafType(String typeCode, String typeName,
                                                     boolean hasCapacity, String capacityUnit,
                                                     boolean bookable, boolean assignable, boolean occupiable) {
        return UniversalSpaceType.builder()
                .typeCode(typeCode)
                .typeName(typeName)
                .isRootType(false)
                .allowedChildTypes(new ArrayList<>())
                .hasCapacity(hasCapacity)
                .capacityUnit(capacityUnit)
                .bookable(bookable)
                .assignable(assignable)
                .occupiable(occupiable)
                .build();
    }
}
