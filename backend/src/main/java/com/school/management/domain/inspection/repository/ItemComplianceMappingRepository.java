package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.compliance.ItemComplianceMapping;

import java.util.List;
import java.util.Optional;

public interface ItemComplianceMappingRepository {

    ItemComplianceMapping save(ItemComplianceMapping mapping);

    Optional<ItemComplianceMapping> findById(Long id);

    List<ItemComplianceMapping> findByItemId(Long templateItemId);

    List<ItemComplianceMapping> findByClauseId(Long clauseId);

    void deleteById(Long id);

    void deleteByItemId(Long templateItemId);
}
