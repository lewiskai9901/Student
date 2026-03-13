package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.rating.InspRatingLink;

import java.util.List;
import java.util.Optional;

public interface InspRatingLinkRepository {

    InspRatingLink save(InspRatingLink link);

    Optional<InspRatingLink> findById(Long id);

    List<InspRatingLink> findByProjectId(Long projectId);

    List<InspRatingLink> findByProjectIdAndPeriodType(Long projectId, String periodType);

    List<InspRatingLink> findByRatingConfigId(Long ratingConfigId);

    void deleteById(Long id);
}
