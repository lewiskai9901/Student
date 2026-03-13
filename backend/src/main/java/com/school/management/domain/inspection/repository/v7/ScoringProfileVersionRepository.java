package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.ScoringProfileVersion;

import java.util.List;
import java.util.Optional;

public interface ScoringProfileVersionRepository {

    ScoringProfileVersion save(ScoringProfileVersion version);

    Optional<ScoringProfileVersion> findById(Long id);

    List<ScoringProfileVersion> findByProfileId(Long profileId);

    Optional<ScoringProfileVersion> findByProfileIdAndVersion(Long profileId, Integer version);

    void deleteByProfileId(Long profileId);
}
