package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateSection;

import java.util.List;
import java.util.Optional;

public interface TemplateSectionRepository {

    TemplateSection save(TemplateSection section);

    Optional<TemplateSection> findById(Long id);

    List<TemplateSection> findByTemplateId(Long templateId);

    void deleteById(Long id);

    void deleteByTemplateId(Long templateId);
}
