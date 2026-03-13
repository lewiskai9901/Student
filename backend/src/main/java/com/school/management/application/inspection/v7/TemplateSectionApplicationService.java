package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.TemplateSectionRepository;
import com.school.management.domain.inspection.repository.v7.TemplateItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TemplateSectionApplicationService {

    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;

    @Transactional
    public TemplateSection createSection(Long templateId, String sectionCode, String sectionName,
                                          Integer weight,
                                          Boolean isRepeatable, String conditionLogic,
                                          Integer sortOrder, Long createdBy) {
        TemplateSection section = TemplateSection.create(templateId, sectionCode, sectionName, createdBy);
        section.update(sectionName, weight, isRepeatable, conditionLogic, createdBy);
        if (sortOrder != null) {
            section.reorder(sortOrder);
        }
        return sectionRepository.save(section);
    }

    @Transactional(readOnly = true)
    public List<TemplateSection> listSections(Long templateId) {
        return sectionRepository.findByTemplateId(templateId);
    }

    @Transactional
    public TemplateSection updateSection(Long id, String sectionName,
                                          Integer weight, Boolean isRepeatable,
                                          String conditionLogic, Long updatedBy) {
        TemplateSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + id));
        section.update(sectionName, weight, isRepeatable, conditionLogic, updatedBy);
        return sectionRepository.save(section);
    }

    @Transactional
    public void deleteSection(Long id) {
        // Cascade delete items in this section
        itemRepository.deleteBySectionId(id);
        sectionRepository.deleteById(id);
    }

    @Transactional
    public void reorderSections(Long templateId, List<Long> sectionIds) {
        for (int i = 0; i < sectionIds.size(); i++) {
            TemplateSection section = sectionRepository.findById(sectionIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("分区不存在"));
            section.reorder(i);
            sectionRepository.save(section);
        }
    }
}
