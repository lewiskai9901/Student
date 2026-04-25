package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.ScoreDimension;

import java.util.List;
import java.util.Optional;

public interface ScoreDimensionRepository {

    ScoreDimension save(ScoreDimension dimension);

    Optional<ScoreDimension> findById(Long id);

    List<ScoreDimension> findByScoringProfileId(Long scoringProfileId);

    void deleteById(Long id);

    void deleteByScoringProfileId(Long scoringProfileId);
}
