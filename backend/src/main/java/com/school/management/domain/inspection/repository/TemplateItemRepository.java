package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.template.TemplateItem;

import java.util.List;
import java.util.Optional;

public interface TemplateItemRepository {

    TemplateItem save(TemplateItem item);

    Optional<TemplateItem> findById(Long id);

    List<TemplateItem> findBySectionId(Long sectionId);

    List<TemplateItem> findByTemplateId(Long templateId);

    void deleteById(Long id);

    void deleteBySectionId(Long sectionId);

    List<TemplateItem> findByLibraryItemIdWithSync(Long libraryItemId);
}
