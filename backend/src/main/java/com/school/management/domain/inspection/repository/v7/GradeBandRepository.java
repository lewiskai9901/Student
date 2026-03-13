package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.GradeBand;

import java.util.List;
import java.util.Optional;

public interface GradeBandRepository {

    GradeBand save(GradeBand gradeBand);

    Optional<GradeBand> findById(Long id);

    List<GradeBand> findByScoringProfileId(Long scoringProfileId);

    List<GradeBand> findByDimensionId(Long dimensionId);

    void deleteById(Long id);

    void deleteByDimensionId(Long dimensionId);

    void deleteByScoringProfileId(Long scoringProfileId);
}
