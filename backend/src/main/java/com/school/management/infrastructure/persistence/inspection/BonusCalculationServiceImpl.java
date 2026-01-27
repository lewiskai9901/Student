package com.school.management.infrastructure.persistence.inspection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.BonusItem;
import com.school.management.domain.inspection.model.BonusMode;
import com.school.management.domain.inspection.service.BonusCalculationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Implementation of BonusCalculationService.
 */
@Service
public class BonusCalculationServiceImpl implements BonusCalculationService {

    private final ObjectMapper objectMapper;

    public BonusCalculationServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public BigDecimal calculateFixedBonus(BonusItem item) {
        if (item.getBonusMode() != BonusMode.FIXED) {
            throw new IllegalArgumentException("Item is not FIXED mode");
        }
        return item.getFixedBonus() != null ? item.getFixedBonus() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateProgressiveBonus(BonusItem item, int consecutiveWeeks) {
        if (item.getBonusMode() != BonusMode.PROGRESSIVE) {
            throw new IllegalArgumentException("Item is not PROGRESSIVE mode");
        }
        if (consecutiveWeeks <= 0) {
            return BigDecimal.ZERO;
        }

        String config = item.getProgressiveConfig();
        if (config == null || config.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            // Expected format: [{"week": 1, "bonus": 1.0}, {"week": 2, "bonus": 2.0}, ...]
            List<Map<String, Object>> tiers = objectMapper.readValue(config, new TypeReference<>() {});

            BigDecimal bonus = BigDecimal.ZERO;
            for (Map<String, Object> tier : tiers) {
                int week = ((Number) tier.get("week")).intValue();
                BigDecimal tierBonus = new BigDecimal(tier.get("bonus").toString());
                if (consecutiveWeeks >= week) {
                    bonus = tierBonus;
                }
            }
            return bonus;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public BigDecimal calculateImprovementBonus(BonusItem item, BigDecimal previousScore, BigDecimal currentScore) {
        if (item.getBonusMode() != BonusMode.IMPROVEMENT) {
            throw new IllegalArgumentException("Item is not IMPROVEMENT mode");
        }
        if (previousScore == null || currentScore == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal improvement = currentScore.subtract(previousScore);
        if (improvement.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal coefficient = item.getImprovementCoefficient() != null
            ? item.getImprovementCoefficient() : BigDecimal.ONE;

        return improvement.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }
}
