package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateVersion;

import java.util.List;
import java.util.Optional;

public interface TemplateVersionRepository {

    TemplateVersion save(TemplateVersion version);

    Optional<TemplateVersion> findById(Long id);

    Optional<TemplateVersion> findByTemplateIdAndVersion(Long templateId, Integer version);

    List<TemplateVersion> findByTemplateId(Long templateId);

    Optional<TemplateVersion> findLatestByTemplateId(Long templateId);
}
