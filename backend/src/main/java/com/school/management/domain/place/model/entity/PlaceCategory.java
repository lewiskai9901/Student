package com.school.management.domain.space.model.entity;

import com.school.management.domain.shared.Entity;
import com.school.management.domain.space.model.valueobject.SpaceLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 空间分类实体
 * 用于配置楼栋分类（教学楼、宿舍楼等）和房间分类（宿舍、教室等）
 */
@Getter
@NoArgsConstructor
public class SpaceCategory implements Entity<Long> {

    private Long id;
    private String categoryCode;
    private String categoryName;
    private SpaceLevel applyToLevel;  // 适用层级：BUILDING 或 ROOM
    private String icon;
    private String color;
    private String description;

    // 行为特性
    private boolean hasCapacity;      // 是否有容量
    private String capacityUnit;      // 容量单位：人/床位/座位
    private Integer defaultCapacity;  // 默认容量
    private boolean bookable;         // 是否可预订
    private boolean assignable;       // 是否可分配给组织/班级
    private boolean occupiable;       // 是否可入住
    private boolean hasGender;        // 是否区分性别（宿舍用）

    // 系统字段
    private boolean isSystem;         // 是否系统预置
    private boolean isEnabled;        // 是否启用
    private Integer sortOrder;

    // 审计字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 创建楼栋分类
     */
    public static SpaceCategory createBuildingCategory(String code, String name, String description) {
        SpaceCategory category = new SpaceCategory();
        category.categoryCode = code;
        category.categoryName = name;
        category.applyToLevel = SpaceLevel.BUILDING;
        category.description = description;
        category.hasCapacity = false;
        category.bookable = false;
        category.assignable = true;
        category.occupiable = false;
        category.hasGender = false;
        category.isSystem = false;
        category.isEnabled = true;
        category.sortOrder = 0;
        category.createdAt = LocalDateTime.now();
        return category;
    }

    /**
     * 创建房间分类
     */
    public static SpaceCategory createRoomCategory(String code, String name, String description,
                                                   boolean hasCapacity, String capacityUnit,
                                                   Integer defaultCapacity,
                                                   boolean bookable, boolean assignable,
                                                   boolean occupiable, boolean hasGender) {
        SpaceCategory category = new SpaceCategory();
        category.categoryCode = code;
        category.categoryName = name;
        category.applyToLevel = SpaceLevel.ROOM;
        category.description = description;
        category.hasCapacity = hasCapacity;
        category.capacityUnit = capacityUnit;
        category.defaultCapacity = defaultCapacity;
        category.bookable = bookable;
        category.assignable = assignable;
        category.occupiable = occupiable;
        category.hasGender = hasGender;
        category.isSystem = false;
        category.isEnabled = true;
        category.sortOrder = 0;
        category.createdAt = LocalDateTime.now();
        return category;
    }

    /**
     * 从持久化数据重建
     */
    public static SpaceCategory reconstitute(Long id, String categoryCode, String categoryName,
                                             SpaceLevel applyToLevel, String icon, String color,
                                             String description,
                                             boolean hasCapacity, String capacityUnit, Integer defaultCapacity,
                                             boolean bookable, boolean assignable, boolean occupiable, boolean hasGender,
                                             boolean isSystem, boolean isEnabled, Integer sortOrder,
                                             Long createdBy, LocalDateTime createdAt,
                                             Long updatedBy, LocalDateTime updatedAt) {
        SpaceCategory category = new SpaceCategory();
        category.id = id;
        category.categoryCode = categoryCode;
        category.categoryName = categoryName;
        category.applyToLevel = applyToLevel;
        category.icon = icon;
        category.color = color;
        category.description = description;
        category.hasCapacity = hasCapacity;
        category.capacityUnit = capacityUnit;
        category.defaultCapacity = defaultCapacity;
        category.bookable = bookable;
        category.assignable = assignable;
        category.occupiable = occupiable;
        category.hasGender = hasGender;
        category.isSystem = isSystem;
        category.isEnabled = isEnabled;
        category.sortOrder = sortOrder;
        category.createdBy = createdBy;
        category.createdAt = createdAt;
        category.updatedBy = updatedBy;
        category.updatedAt = updatedAt;
        return category;
    }

    // ========== 业务方法 ==========

    public void setId(Long id) {
        this.id = id;
    }

    public void update(String name, String description, String icon, String color,
                       boolean hasCapacity, String capacityUnit, Integer defaultCapacity,
                       boolean bookable, boolean assignable, boolean occupiable, boolean hasGender,
                       Integer sortOrder) {
        this.categoryName = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.hasCapacity = hasCapacity;
        this.capacityUnit = capacityUnit;
        this.defaultCapacity = defaultCapacity;
        this.bookable = bookable;
        this.assignable = assignable;
        this.occupiable = occupiable;
        this.hasGender = hasGender;
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void setCreatedBy(Long userId) {
        this.createdBy = userId;
    }

    public void setUpdatedBy(Long userId) {
        this.updatedBy = userId;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 查询方法 ==========

    public boolean isBuildingCategory() {
        return this.applyToLevel == SpaceLevel.BUILDING;
    }

    public boolean isRoomCategory() {
        return this.applyToLevel == SpaceLevel.ROOM;
    }

    public boolean isDormitoryCategory() {
        return isRoomCategory() && this.occupiable && this.hasGender;
    }

    public boolean isClassroomCategory() {
        return isRoomCategory() && this.hasCapacity && this.bookable && !this.occupiable;
    }
}
