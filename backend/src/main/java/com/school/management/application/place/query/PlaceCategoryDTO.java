package com.school.management.application.place.query;

import com.school.management.domain.place.model.entity.PlaceCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 空间分类DTO
 */
@Data
public class PlaceCategoryDTO {
    private Long id;
    private String categoryCode;
    private String categoryName;
    private String applyToLevel;
    private String applyToLevelDesc;
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

    /**
     * 从领域对象构建DTO
     */
    public static PlaceCategoryDTO fromDomain(PlaceCategory category) {
        if (category == null) {
            return null;
        }

        PlaceCategoryDTO dto = new PlaceCategoryDTO();
        dto.setId(category.getId());
        dto.setCategoryCode(category.getCategoryCode());
        dto.setCategoryName(category.getCategoryName());
        if (category.getApplyToLevel() != null) {
            dto.setApplyToLevel(category.getApplyToLevel().name());
            dto.setApplyToLevelDesc(category.getApplyToLevel().getDescription());
        }
        dto.setIcon(category.getIcon());
        dto.setColor(category.getColor());
        dto.setDescription(category.getDescription());
        dto.setHasCapacity(category.isHasCapacity());
        dto.setCapacityUnit(category.getCapacityUnit());
        dto.setDefaultCapacity(category.getDefaultCapacity());
        dto.setBookable(category.isBookable());
        dto.setAssignable(category.isAssignable());
        dto.setOccupiable(category.isOccupiable());
        dto.setHasGender(category.isHasGender());
        dto.setIsSystem(category.isSystem());
        dto.setIsEnabled(category.isEnabled());
        dto.setSortOrder(category.getSortOrder());
        dto.setCreatedBy(category.getCreatedBy());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedBy(category.getUpdatedBy());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
}
