package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.TemplateSectionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TemplateSectionRepositoryImpl implements TemplateSectionRepository {

    private final TemplateSectionMapper mapper;

    public TemplateSectionRepositoryImpl(TemplateSectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateSection save(TemplateSection section) {
        TemplateSectionPO po = toPO(section);
        if (section.getId() == null) {
            mapper.insert(po);
            section.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return section;
    }

    @Override
    public Optional<TemplateSection> findById(Long id) {
        TemplateSectionPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TemplateSection> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByTemplateId(Long templateId) {
        mapper.softDeleteByTemplateId(templateId);
    }

    private TemplateSectionPO toPO(TemplateSection domain) {
        TemplateSectionPO po = new TemplateSectionPO();
        po.setId(domain.getId());
        po.setTenantId(0L);
        po.setTemplateId(domain.getTemplateId());
        po.setSectionCode(domain.getSectionCode());
        po.setSectionName(domain.getSectionName());
        po.setSortOrder(domain.getSortOrder());
        po.setWeight(domain.getWeight());
        po.setIsRepeatable(domain.getIsRepeatable());
        po.setConditionLogic(domain.getConditionLogic());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private TemplateSection toDomain(TemplateSectionPO po) {
        return TemplateSection.reconstruct(TemplateSection.builder()
                .id(po.getId())
                .templateId(po.getTemplateId())
                .sectionCode(po.getSectionCode())
                .sectionName(po.getSectionName())
                .sortOrder(po.getSortOrder())
                .weight(po.getWeight())
                .isRepeatable(po.getIsRepeatable())
                .conditionLogic(po.getConditionLogic())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
