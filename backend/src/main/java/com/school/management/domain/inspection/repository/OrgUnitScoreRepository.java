package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.OrgUnitScore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrgUnitScoreRepository {

    OrgUnitScore save(OrgUnitScore score);

    Optional<OrgUnitScore> findById(Long id);

    Optional<OrgUnitScore> findByProjectIdAndOrgUnitIdAndCycleDate(Long projectId, Long orgUnitId, LocalDate cycleDate);

    List<OrgUnitScore> findByProjectIdAndCycleDate(Long projectId, LocalDate cycleDate);

    List<OrgUnitScore> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
