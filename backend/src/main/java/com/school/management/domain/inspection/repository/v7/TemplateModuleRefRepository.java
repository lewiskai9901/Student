package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateModuleRef;

import java.util.List;
import java.util.Optional;

public interface TemplateModuleRefRepository {

    TemplateModuleRef save(TemplateModuleRef ref);

    Optional<TemplateModuleRef> findById(Long id);

    List<TemplateModuleRef> findByCompositeTemplateId(Long compositeTemplateId);

    void deleteById(Long id);

    void deleteByCompositeTemplateId(Long compositeTemplateId);
}
