package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.GradeDefinition;

import java.util.List;

public interface GradeDefinitionRepository {

    GradeDefinition save(GradeDefinition def);

    List<GradeDefinition> findByGradeSchemeId(Long gradeSchemeId);

    void deleteByGradeSchemeId(Long gradeSchemeId);
}
