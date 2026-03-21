package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.EvaluationLevel;

import java.util.List;

public interface EvaluationLevelRepository {

    EvaluationLevel save(EvaluationLevel level);

    List<EvaluationLevel> findByRuleId(Long ruleId);

    void deleteByRuleId(Long ruleId);
}
