package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.ScoringPreset;

import java.util.List;
import java.util.Optional;

public interface ScoringPresetRepository {

    ScoringPreset save(ScoringPreset preset);

    Optional<ScoringPreset> findById(Long id);

    List<ScoringPreset> findByTemplateId(Long templateId);

    void deleteById(Long id);
}
