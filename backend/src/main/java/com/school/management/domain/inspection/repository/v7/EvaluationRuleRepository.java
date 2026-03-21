package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.EvaluationRule;

import java.util.List;
import java.util.Optional;

public interface EvaluationRuleRepository {

    EvaluationRule save(EvaluationRule rule);

    Optional<EvaluationRule> findById(Long id);

    List<EvaluationRule> findByProjectId(Long projectId);

    void deleteById(Long id);
}
