package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间分类持久化对象
 */
@Data
@TableName("place_categories")
public class PlaceCategoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    // 系统字段
    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;

    // 审计字段
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
