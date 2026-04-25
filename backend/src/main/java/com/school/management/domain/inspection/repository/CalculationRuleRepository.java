package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.CalculationRule;

import java.util.List;
import java.util.Optional;

public interface CalculationRuleRepository {

    CalculationRule save(CalculationRule rule);

    Optional<CalculationRule> findById(Long id);

    List<CalculationRule> findByScoringProfileId(Long scoringProfileId);

    List<CalculationRule> findByScoringProfileIdOrderByPriority(Long scoringProfileId);

    void deleteById(Long id);

    void deleteByScoringProfileId(Long scoringProfileId);
}
