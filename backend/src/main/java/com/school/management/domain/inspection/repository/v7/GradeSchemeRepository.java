package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;

import java.util.List;
import java.util.Optional;

public interface GradeSchemeRepository {

    GradeScheme save(GradeScheme scheme);

    Optional<GradeScheme> findById(Long id);

    List<GradeScheme> findByTenantIdOrSystem(Long tenantId);

    void deleteById(Long id);
}
