package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateCatalog;

import java.util.List;
import java.util.Optional;

public interface TemplateCatalogRepository {

    TemplateCatalog save(TemplateCatalog catalog);

    Optional<TemplateCatalog> findById(Long id);

    Optional<TemplateCatalog> findByCatalogCode(String catalogCode);

    List<TemplateCatalog> findAll();

    List<TemplateCatalog> findByParentId(Long parentId);

    void deleteById(Long id);
}
