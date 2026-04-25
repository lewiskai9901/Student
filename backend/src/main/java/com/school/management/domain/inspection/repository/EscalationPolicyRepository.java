package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.EscalationPolicy;

import java.util.List;
import java.util.Optional;

public interface EscalationPolicyRepository {
    EscalationPolicy save(EscalationPolicy policy);
    Optional<EscalationPolicy> findById(Long id);
    List<EscalationPolicy> findByProfileId(Long profileId);
    void deleteById(Long id);
    void deleteByProfileId(Long profileId);
}
