package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.compliance.ComplianceClause;

import java.util.List;
import java.util.Optional;

public interface ComplianceClauseRepository {

    ComplianceClause save(ComplianceClause clause);

    Optional<ComplianceClause> findById(Long id);

    List<ComplianceClause> findByStandardId(Long standardId);

    void deleteById(Long id);

    void deleteByStandardId(Long standardId);
}
