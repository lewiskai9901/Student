package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.ScoringProfile;

import java.util.List;
import java.util.Optional;

public interface ScoringProfileRepository {

    ScoringProfile save(ScoringProfile profile);

    Optional<ScoringProfile> findById(Long id);

    Optional<ScoringProfile> findByTemplateId(Long templateId);

    List<ScoringProfile> findAll();

    void deleteById(Long id);
}
