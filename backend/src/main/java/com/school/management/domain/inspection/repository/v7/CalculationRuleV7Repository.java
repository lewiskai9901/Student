package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.CalculationRuleV7;

import java.util.List;
import java.util.Optional;

public interface CalculationRuleV7Repository {

    CalculationRuleV7 save(CalculationRuleV7 rule);

    Optional<CalculationRuleV7> findById(Long id);

    List<CalculationRuleV7> findByScoringProfileId(Long scoringProfileId);

    List<CalculationRuleV7> findByScoringProfileIdOrderByPriority(Long scoringProfileId);

    void deleteById(Long id);

    void deleteByScoringProfileId(Long scoringProfileId);
}
