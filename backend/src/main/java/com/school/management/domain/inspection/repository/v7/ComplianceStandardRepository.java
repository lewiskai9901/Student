package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.compliance.ComplianceStandard;

import java.util.List;
import java.util.Optional;

public interface ComplianceStandardRepository {

    ComplianceStandard save(ComplianceStandard standard);

    Optional<ComplianceStandard> findById(Long id);

    List<ComplianceStandard> findAll();

    Optional<ComplianceStandard> findByCode(String standardCode);

    void deleteById(Long id);
}
