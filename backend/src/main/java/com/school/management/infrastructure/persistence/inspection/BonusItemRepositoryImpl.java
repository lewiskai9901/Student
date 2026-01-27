package com.school.management.infrastructure.persistence.inspection;

import com.school.management.domain.inspection.model.BonusItem;
import com.school.management.domain.inspection.model.BonusMode;
import com.school.management.domain.inspection.repository.BonusItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis Plus implementation of BonusItemRepository.
 */
@Repository
public class BonusItemRepositoryImpl implements BonusItemRepository {

    private final BonusItemMapper bonusItemMapper;

    public BonusItemRepositoryImpl(BonusItemMapper bonusItemMapper) {
        this.bonusItemMapper = bonusItemMapper;
    }

    @Override
    public BonusItem save(BonusItem aggregate) {
        BonusItemPO po = toPO(aggregate);
        if (aggregate.getId() == null) {
            bonusItemMapper.insert(po);
            aggregate.setId(po.getId());
        } else {
            bonusItemMapper.updateById(po);
        }
        return aggregate;
    }

    @Override
    public Optional<BonusItem> findById(Long id) {
        BonusItemPO po = bonusItemMapper.selectById(id);
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void delete(BonusItem aggregate) {
        if (aggregate != null && aggregate.getId() != null) {
            bonusItemMapper.deleteById(aggregate.getId());
        }
    }

    @Override
    public List<BonusItem> findByCategoryId(Long categoryId) {
        return bonusItemMapper.findByCategoryId(categoryId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<BonusItem> findAllEnabled() {
        return bonusItemMapper.findAllEnabled().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    // ==================== Mapping Methods ====================

    private BonusItemPO toPO(BonusItem domain) {
        BonusItemPO po = new BonusItemPO();
        po.setId(domain.getId());
        po.setCategoryId(domain.getCategoryId());
        po.setItemName(domain.getItemName());
        po.setBonusMode(domain.getBonusMode().name());
        po.setFixedBonus(domain.getFixedBonus());
        po.setProgressiveConfig(domain.getProgressiveConfig());
        po.setImprovementCoefficient(domain.getImprovementCoefficient());
        po.setDescription(domain.getDescription());
        po.setSortOrder(domain.getSortOrder());
        po.setStatus(domain.getStatus());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private BonusItem toDomain(BonusItemPO po) {
        return BonusItem.builder()
            .id(po.getId())
            .categoryId(po.getCategoryId())
            .itemName(po.getItemName())
            .bonusMode(BonusMode.valueOf(po.getBonusMode()))
            .fixedBonus(po.getFixedBonus())
            .progressiveConfig(po.getProgressiveConfig())
            .improvementCoefficient(po.getImprovementCoefficient())
            .description(po.getDescription())
            .sortOrder(po.getSortOrder())
            .status(po.getStatus())
            .createdAt(po.getCreatedAt())
            .updatedAt(po.getUpdatedAt())
            .build();
    }
}
