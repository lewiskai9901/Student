package com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository;

import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.CohortSemesterMapping;
import java.util.List;
import java.util.Optional;

public interface CohortSemesterMappingRepository {
    CohortSemesterMapping save(CohortSemesterMapping mapping);
    Optional<CohortSemesterMapping> findByCohortAndSemester(Long cohortId, Long semesterId);
    List<CohortSemesterMapping> findBySemesterId(Long semesterId);
    List<CohortSemesterMapping> findByCohortId(Long cohortId);
    void deleteById(Long id);
}
