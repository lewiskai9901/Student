package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.template.InspTemplate;
import com.school.management.domain.inspection.model.template.TemplateStatus;

import java.util.List;
import java.util.Optional;

public interface InspTemplateRepository {

    InspTemplate save(InspTemplate template);

    Optional<InspTemplate> findById(Long id);

    Optional<InspTemplate> findByTemplateCode(String templateCode);

    List<InspTemplate> findByStatus(TemplateStatus status);

    List<InspTemplate> findByCatalogId(Long catalogId);

    List<InspTemplate> findPagedWithConditions(int offset, int size,
                                                TemplateStatus status, Long catalogId, String keyword);

    long countWithConditions(TemplateStatus status, Long catalogId, String keyword);

    void deleteById(Long id);
}
