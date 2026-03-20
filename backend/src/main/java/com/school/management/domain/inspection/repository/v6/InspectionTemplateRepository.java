package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.InspectionTemplate;

import java.util.List;
import java.util.Optional;

public interface InspectionTemplateRepository {

    InspectionTemplate save(InspectionTemplate template);

    Optional<InspectionTemplate> findById(Long id);

    List<InspectionTemplate> findAll();

    void deleteById(Long id);
}
