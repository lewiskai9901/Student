package com.school.management.application.inspection;

import com.school.management.application.inspection.command.CreateBonusItemCommand;
import com.school.management.domain.inspection.model.BonusItem;
import com.school.management.domain.inspection.model.BonusMode;
import com.school.management.domain.inspection.repository.BonusItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for bonus item CRUD operations.
 */
@Service
@Transactional
public class BonusItemApplicationService {

    private final BonusItemRepository bonusItemRepository;

    public BonusItemApplicationService(BonusItemRepository bonusItemRepository) {
        this.bonusItemRepository = bonusItemRepository;
    }

    public BonusItem createBonusItem(CreateBonusItemCommand command) {
        BonusItem item = BonusItem.create(
            command.getCategoryId(),
            command.getItemName(),
            command.getBonusMode(),
            command.getFixedBonus(),
            command.getProgressiveConfig(),
            command.getImprovementCoefficient(),
            command.getDescription()
        );
        if (command.getSortOrder() != null) {
            item.update(item.getItemName(), item.getBonusMode(), item.getFixedBonus(),
                item.getProgressiveConfig(), item.getImprovementCoefficient(),
                item.getDescription(), command.getSortOrder());
        }
        return bonusItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Optional<BonusItem> getBonusItem(Long id) {
        return bonusItemRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<BonusItem> listByCategoryId(Long categoryId) {
        return bonusItemRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public List<BonusItem> listAllEnabled() {
        return bonusItemRepository.findAllEnabled();
    }

    public BonusItem updateBonusItem(Long id, String itemName, BonusMode bonusMode,
                                      java.math.BigDecimal fixedBonus, String progressiveConfig,
                                      java.math.BigDecimal improvementCoefficient,
                                      String description, Integer sortOrder) {
        BonusItem item = bonusItemRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Bonus item not found: " + id));
        item.update(itemName, bonusMode, fixedBonus, progressiveConfig,
            improvementCoefficient, description, sortOrder);
        return bonusItemRepository.save(item);
    }

    public void deleteBonusItem(Long id) {
        bonusItemRepository.deleteById(id);
    }
}
