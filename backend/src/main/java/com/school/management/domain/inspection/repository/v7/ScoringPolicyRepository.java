package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.ScoringPolicy;

import java.util.List;
import java.util.Optional;

public interface ScoringPolicyRepository {

    ScoringPolicy save(ScoringPolicy policy);

    Optional<ScoringPolicy> findById(Long id);

    Optional<ScoringPolicy> findByCode(String policyCode);

    List<ScoringPolicy> findAll();

    List<ScoringPolicy> findEnabled();

    void deleteById(Long id);
}
