package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.analytics.AlertRule;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository {

    AlertRule save(AlertRule alertRule);

    Optional<AlertRule> findById(Long id);

    List<AlertRule> findAll();

    List<AlertRule> findEnabled();

    void deleteById(Long id);
}
