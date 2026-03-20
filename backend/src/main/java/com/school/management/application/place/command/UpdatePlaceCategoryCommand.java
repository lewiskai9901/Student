package com.school.management.application.place.command;

import lombok.Data;

/**
 * 更新空间分类命令
 */
@Data
public class UpdatePlaceCategoryCommand {
    private Long id;
    private String categoryName;
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
