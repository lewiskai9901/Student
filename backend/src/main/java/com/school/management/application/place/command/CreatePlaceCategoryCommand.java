package com.school.management.application.place.command;

import lombok.Data;

/**
 * 创建空间分类命令
 */
@Data
public class CreatePlaceCategoryCommand {
    private String categoryCode;
    private String categoryName;
    private String applyToLevel;  // BUILDING 或 ROOM
    private String icon;
    private String color;
    private String description;

    // 行为特性
    private Boolean hasCapacity;
    private String capacityUnit;
    private Integer defaultCapacity;
    private Boolean bookable;
    private Boolean assignable;
    private Boolean occupiable;
    private Boolean hasGender;

    private Integer sortOrder;
}
