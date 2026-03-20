package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.RatingDimension;

import java.util.List;
import java.util.Optional;

public interface RatingDimensionRepository {

    RatingDimension save(RatingDimension dimension);

    Optional<RatingDimension> findById(Long id);

    List<RatingDimension> findByProjectId(Long projectId);

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);
}
