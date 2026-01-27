package com.school.management.domain.corrective.repository;

import com.school.management.domain.corrective.model.AutoActionRule;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AutoActionRule entity.
 */
public interface AutoActionRuleRepository extends Repository<AutoActionRule, Long> {

    List<AutoActionRule> findEnabled();

    Optional<AutoActionRule> findByRuleCode(String ruleCode);

    List<AutoActionRule> findByTriggerType(String triggerType);
}
