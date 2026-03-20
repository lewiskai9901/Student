package com.school.management.infrastructure.persistence.place;

import com.school.management.domain.place.model.entity.PlaceCategory;
import com.school.management.domain.place.model.valueobject.PlaceLevel;
import org.springframework.stereotype.Component;

/**
 * 空间分类领域对象与持久化对象映射器
 */
@Component
public class PlaceCategoryDomainMapper {

    /**
     * PO -> 领域对象
     */
    public PlaceCategory toDomain(PlaceCategoryPO po) {
        if (po == null) {
            return null;
        }

        PlaceLevel applyToLevel = po.getApplyToLevel() != null
            ? PlaceLevel.valueOf(po.getApplyToLevel())
            : null;

        return PlaceCategory.reconstitute(
            po.getId(),
            po.getCategoryCode(),
            po.getCategoryName(),
            applyToLevel,
            po.getIcon(),
            po.getColor(),
            po.getDescription(),
            Boolean.TRUE.equals(po.getHasCapacity()),
            po.getCapacityUnit(),
            po.getDefaultCapacity(),
            Boolean.TRUE.equals(po.getBookable()),
            Boolean.TRUE.equals(po.getAssignable()),
            Boolean.TRUE.equals(po.getOccupiable()),
            Boolean.TRUE.equals(po.getHasGender()),
            Boolean.TRUE.equals(po.getIsSystem()),
            Boolean.TRUE.equals(po.getIsEnabled()),
            po.getSortOrder(),
            po.getCreatedBy(),
            po.getCreatedAt(),
            po.getUpdatedBy(),
            po.getUpdatedAt()
        );
    }

    /**
     * 领域对象 -> PO
     */
    public PlaceCategoryPO toPO(PlaceCategory category) {
        if (category == null) {
            return null;
        }

        PlaceCategoryPO po = new PlaceCategoryPO();
        po.setId(category.getId());
        po.setCategoryCode(category.getCategoryCode());
        po.setCategoryName(category.getCategoryName());
        po.setApplyToLevel(category.getApplyToLevel() != null
            ? category.getApplyToLevel().name() : null);
        po.setIcon(category.getIcon());
        po.setColor(category.getColor());
        po.setDescription(category.getDescription());
        po.setHasCapacity(category.isHasCapacity());
        po.setCapacityUnit(category.getCapacityUnit());
        po.setDefaultCapacity(category.getDefaultCapacity());
        po.setBookable(category.isBookable());
        po.setAssignable(category.isAssignable());
        po.setOccupiable(category.isOccupiable());
        po.setHasGender(category.isHasGender());
        po.setIsSystem(category.isSystem());
        po.setIsEnabled(category.isEnabled());
        po.setSortOrder(category.getSortOrder());
        po.setCreatedBy(category.getCreatedBy());
        po.setCreatedAt(category.getCreatedAt());
        po.setUpdatedBy(category.getUpdatedBy());
        po.setUpdatedAt(category.getUpdatedAt());
        return po;
    }
}
