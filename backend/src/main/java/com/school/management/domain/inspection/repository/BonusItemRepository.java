package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.BonusItem;
import com.school.management.domain.shared.Repository;

import java.util.List;

/**
 * Repository interface for BonusItem entity.
 */
public interface BonusItemRepository extends Repository<BonusItem, Long> {

    List<BonusItem> findByCategoryId(Long categoryId);

    List<BonusItem> findAllEnabled();
}
