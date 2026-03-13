package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.*;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.common.PageResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InspTemplateApplicationService {

    private final InspTemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;
    private final TemplateModuleRefRepository moduleRefRepository;
    private final SpringDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public InspTemplate createTemplate(String templateName, String description,
                                        Long catalogId, String tags,
                                        TargetType targetType, Long createdBy) {
        String templateCode = generateTemplateCode();
        InspTemplate template = InspTemplate.create(templateCode, templateName, createdBy);

        // Set optional fields via update
        if (description != null || catalogId != null || tags != null || targetType != null) {
            InspTemplate saved = templateRepository.save(template);
            saved.updateInfo(templateName, description, catalogId, tags, targetType, createdBy);
            return templateRepository.save(saved);
        }

        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public Optional<InspTemplate> getTemplate(Long id) {
        return templateRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public PageResult<InspTemplate> listTemplates(int page, int size, TemplateStatus status,
                                                   Long catalogId, String keyword) {
        int offset = (page - 1) * size;
        List<InspTemplate> templates = templateRepository.findPagedWithConditions(offset, size, status, catalogId, keyword);
        long total = templateRepository.countWithConditions(status, catalogId, keyword);
        return PageResult.of(templates, total, page, size);
    }

    @Transactional
    public InspTemplate updateTemplate(Long id, String templateName, String description,
                                        Long catalogId, String tags,
                                        TargetType targetType, Long updatedBy) {
        InspTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        template.updateInfo(templateName, description, catalogId, tags, targetType, updatedBy);
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        InspTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        if (template.getStatus() != TemplateStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的模板才能删除");
        }
        // Cascade delete sections and items
        List<TemplateSection> sections = sectionRepository.findByTemplateId(id);
        for (TemplateSection section : sections) {
            itemRepository.deleteBySectionId(section.getId());
        }
        sectionRepository.deleteByTemplateId(id);
        templateRepository.deleteById(id);
    }

    @Transactional
    public TemplateVersion publishTemplate(Long id, Long operatorId) {
        InspTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        List<TemplateSection> sections = sectionRepository.findByTemplateId(id);
        List<TemplateItem> items = itemRepository.findByTemplateId(id);
        List<TemplateModuleRef> moduleRefs = moduleRefRepository.findByCompositeTemplateId(id);

        if (sections.isEmpty() && moduleRefs.isEmpty()) {
            throw new IllegalArgumentException("模板至少需要一个分区或子模板引用才能发布");
        }

        // Build structure snapshot
        String structureSnapshot;
        try {
            Map<String, Object> structure = new HashMap<>();
            structure.put("sections", sections);
            structure.put("items", items);
            structure.put("moduleRefs", moduleRefs);
            structureSnapshot = objectMapper.writeValueAsString(structure);
        } catch (Exception e) {
            throw new RuntimeException("序列化模板结构失败", e);
        }

        TemplateVersion version = template.publish(sections, items, structureSnapshot, null);
        templateRepository.save(template);
        versionRepository.save(version);

        // Publish domain events
        template.getDomainEvents().forEach(eventPublisher::publish);
        template.clearDomainEvents();

        return version;
    }

    @Transactional
    public void deprecateTemplate(Long id) {
        InspTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        template.deprecate();
        templateRepository.save(template);
    }

    @Transactional
    public void archiveTemplate(Long id) {
        InspTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        template.archive();
        templateRepository.save(template);
    }

    @Transactional
    public InspTemplate duplicateTemplate(Long id, Long operatorId) {
        InspTemplate source = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));

        String newCode = generateTemplateCode();
        InspTemplate copy = InspTemplate.create(newCode, source.getTemplateName() + " (副本)", operatorId);
        copy = templateRepository.save(copy);
        copy.updateInfo(copy.getTemplateName(), source.getDescription(), source.getCatalogId(),
                source.getTags(), source.getTargetType(), operatorId);
        copy = templateRepository.save(copy);

        // Copy sections
        List<TemplateSection> sourceSections = sectionRepository.findByTemplateId(id);
        Map<Long, Long> sectionIdMap = new HashMap<>(); // old -> new

        for (TemplateSection srcSection : sourceSections) {
            TemplateSection newSection = TemplateSection.create(
                    copy.getId(), srcSection.getSectionCode(), srcSection.getSectionName(), operatorId);
            newSection.update(srcSection.getSectionName(), srcSection.getWeight(),
                    srcSection.getIsRepeatable(), srcSection.getConditionLogic(), operatorId);
            newSection.reorder(srcSection.getSortOrder());
            newSection = sectionRepository.save(newSection);
            sectionIdMap.put(srcSection.getId(), newSection.getId());
        }

        // Copy items
        for (TemplateSection srcSection : sourceSections) {
            Long newSectionId = sectionIdMap.get(srcSection.getId());
            List<TemplateItem> srcItems = itemRepository.findBySectionId(srcSection.getId());
            for (TemplateItem srcItem : srcItems) {
                TemplateItem newItem = TemplateItem.create(
                        newSectionId, srcItem.getItemCode(), srcItem.getItemName(),
                        srcItem.getItemType(), operatorId);
                newItem.update(srcItem.getItemName(), srcItem.getDescription(), srcItem.getItemType(),
                        srcItem.getConfig(), srcItem.getValidationRules(), srcItem.getResponseSetId(),
                        srcItem.getScoringConfig(), srcItem.getDimensionId(), srcItem.getHelpContent(),
                        srcItem.getIsRequired(), srcItem.getIsScored(), srcItem.getRequireEvidence(),
                        srcItem.getItemWeight(), srcItem.getConditionLogic(), operatorId);
                newItem.reorder(srcItem.getSortOrder());
                itemRepository.save(newItem);
            }
        }

        return copy;
    }

    @Transactional(readOnly = true)
    public List<TemplateVersion> listVersions(Long templateId) {
        return versionRepository.findByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public Optional<TemplateVersion> getVersion(Long templateId, Integer version) {
        return versionRepository.findByTemplateIdAndVersion(templateId, version);
    }

    @Transactional(readOnly = true)
    public String exportTemplate(Long id) {
        InspTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + id));
        List<TemplateSection> sections = sectionRepository.findByTemplateId(id);
        List<TemplateItem> items = itemRepository.findByTemplateId(id);

        try {
            Map<String, Object> export = new LinkedHashMap<>();
            export.put("template", template);
            export.put("sections", sections);
            export.put("items", items);
            return objectMapper.writeValueAsString(export);
        } catch (Exception e) {
            throw new RuntimeException("导出模板失败", e);
        }
    }

    private String generateTemplateCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TPL-" + datePart + "-" + randomPart;
    }
}
