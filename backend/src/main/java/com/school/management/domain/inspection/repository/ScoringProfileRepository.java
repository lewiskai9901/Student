package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.ScoringProfile;

import java.util.List;
import java.util.Optional;

public interface ScoringProfileRepository {

    ScoringProfile save(ScoringProfile profile);

    Optional<ScoringProfile> findById(Long id);

    Optional<ScoringProfile> findBySectionId(Long sectionId);

    List<ScoringProfile> findAll();

    void deleteById(Long id);
}
