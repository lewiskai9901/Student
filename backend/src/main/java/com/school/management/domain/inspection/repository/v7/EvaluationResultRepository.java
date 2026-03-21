package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.EvaluationResult;

import java.time.LocalDate;
import java.util.List;

public interface EvaluationResultRepository {

    EvaluationResult save(EvaluationResult result);

    List<EvaluationResult> findByRuleIdAndCycleDate(Long ruleId, LocalDate cycleDate);

    void deleteByRuleIdAndCycleDate(Long ruleId, LocalDate cycleDate);
}
